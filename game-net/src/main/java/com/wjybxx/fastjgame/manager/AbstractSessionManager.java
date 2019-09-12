/*
 *  Copyright 2019 wjybxx
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to iBn writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.wjybxx.fastjgame.manager;

import com.google.inject.Inject;
import com.wjybxx.fastjgame.concurrent.EventLoop;
import com.wjybxx.fastjgame.net.*;
import com.wjybxx.fastjgame.utils.CollectionUtils;
import com.wjybxx.fastjgame.utils.ConcurrentUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * 实现session管理器的通用功能
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/9/9
 * github - https://github.com/hl845740757
 */
public abstract class AbstractSessionManager implements SessionManager {

    protected final NetTimeManager netTimeManager;

    /**
     * 这是一项特定的优化：消除大量的lambda表达式对象，你通常并不需要这样。
     */
    protected final Predicate<RpcPromiseInfo> timeoutPredicate;
    /**
     * 这是一项特定的优化：消除大量的lambda表达式对象，你通常并不需要这样。
     */
    protected final TimeoutConsumer timeoutConsumer;

    @Inject
    protected AbstractSessionManager(NetTimeManager netTimeManager) {
        this.netTimeManager = netTimeManager;
        this.timeoutPredicate = new TimeoutPredicate();
        this.timeoutConsumer = new TimeoutConsumer();
    }

    protected class TimeoutPredicate implements Predicate<RpcPromiseInfo> {

        @Override
        public boolean test(RpcPromiseInfo rpcPromiseInfo) {
            return netTimeManager.getSystemMillTime() >= rpcPromiseInfo.deadline;
        }
    }

    protected class TimeoutConsumer implements Consumer<RpcPromiseInfo> {

        private Session session;

        @Override
        public void accept(RpcPromiseInfo rpcPromiseInfo) {
            commitRpcResponse(session, rpcPromiseInfo, RpcResponse.TIMEOUT);
            // 避免内存泄漏
            session = null;
        }

        public void setSession(@Nonnull Session session) {
            this.session = session;
        }
    }

    // ---------------------------------------------------------- 发送消息  -------------------------------------------------------------

    /**
     * 获取一个可写的session
     *
     * @param localGuid  本地角色guid
     * @param remoteGuid 远程角色guid
     * @return wrapper
     */
    @Nullable
    protected abstract ISessionWrapper getWritableSession(long localGuid, long remoteGuid);

    @Override
    public final void sendOneWayMessage(long localGuid, long remoteGuid, @Nonnull Object message, boolean immediate) {
        final ISessionWrapper sessionWrapper = getWritableSession(localGuid, remoteGuid);
        if (null != sessionWrapper) {
            sessionWrapper.sendOneWayMessage(message, immediate);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public final void sendRpcRequest(long localGuid, long remoteGuid, @Nonnull Object request, long timeoutMs, EventLoop userEventLoop, RpcCallback rpcCallback, boolean immediate) {
        final ISessionWrapper sessionWrapper = getWritableSession(localGuid, remoteGuid);
        if (null != sessionWrapper) {
            // 保存rpc请求上下文
            long deadline = netTimeManager.getSystemMillTime() + timeoutMs;
            RpcPromiseInfo rpcPromiseInfo = RpcPromiseInfo.newInstance(rpcCallback, deadline);

            long requestGuid = sessionWrapper.nextRequestGuid();
            sessionWrapper.getRpcPromiseInfoMap().put(requestGuid, rpcPromiseInfo);
            // 执行发送
            sessionWrapper.sendRpcRequest(requestGuid, immediate, request);
        } else {
            ConcurrentUtils.tryCommit(userEventLoop, () -> {
                rpcCallback.onComplete(RpcResponse.SESSION_CLOSED);
            });
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public final void sendRpcRequest(long localGuid, long remoteGuid, @Nonnull Object request, long timeoutMs, RpcPromise rpcPromise, boolean immediate) {
        final ISessionWrapper sessionWrapper = getWritableSession(localGuid, remoteGuid);
        if (null != sessionWrapper) {
            // 保存rpc请求上下文
            long deadline = netTimeManager.getSystemMillTime() + timeoutMs;
            RpcPromiseInfo rpcPromiseInfo = RpcPromiseInfo.newInstance(rpcPromise, deadline);

            long requestGuid = sessionWrapper.nextRequestGuid();
            sessionWrapper.getRpcPromiseInfoMap().put(requestGuid, rpcPromiseInfo);
            // 执行发送
            sessionWrapper.sendRpcRequest(requestGuid, immediate, request);
        } else {
            rpcPromise.trySuccess(RpcResponse.SESSION_CLOSED);
        }
    }

    @Override
    public final void sendRpcResponse(long localGuid, long remoteGuid, long requestGuid, boolean immediate, @Nonnull RpcResponse response) {
        ISessionWrapper sessionWrapper = getWritableSession(localGuid, remoteGuid);
        if (sessionWrapper != null) {
            sessionWrapper.sendRpcResponse(requestGuid, immediate, response);
        }
    }

    // ----------------------------------------------------------- 提交消息 --------------------------------------------------------------

    /**
     * 立即提交一个消息给应用层
     *
     * @param session    会话信息
     * @param commitTask 准备提交的消息
     */
    protected final void commit(Session session, CommitTask commitTask) {
        if (session.isActive()) {
            ConcurrentUtils.tryCommit(session.localEventLoop(), commitTask);
        }
        // else 丢弃
    }

    /**
     * 提交一个rpc响应结果。
     * rpc调用必须返回一个结果，但是会话关闭的情况下，不能提交真实结果。
     * why？
     * 因为在会话关闭的情况下，单向消息、rpc请求全部被丢弃了，如果提交真实的rpc响应，会导致应用层收到消息的顺序和发送方不一样！！！
     * session关闭的状态下，要么都提交，要么都不提交，不能选择性的提交。
     *
     * @param session        会话信息
     * @param rpcPromiseInfo rpc请求的一些信息
     * @param rpcResponse    rpc结果
     */
    protected final void commitRpcResponse(Session session, RpcPromiseInfo rpcPromiseInfo, RpcResponse rpcResponse) {
        if (rpcPromiseInfo.rpcPromise != null) {
            if (session.isActive()) {
                rpcPromiseInfo.rpcPromise.trySuccess(rpcResponse);
            } else {
                rpcPromiseInfo.rpcPromise.trySuccess(RpcResponse.SESSION_CLOSED);
            }
        } else {
            RpcResponseCommitTask rpcResponseCommitTask;
            if (session.isActive()) {
                rpcResponseCommitTask = new RpcResponseCommitTask(rpcResponse, rpcPromiseInfo.rpcCallback);
            } else {
                rpcResponseCommitTask = new RpcResponseCommitTask(RpcResponse.SESSION_CLOSED, rpcPromiseInfo.rpcCallback);
            }
            ConcurrentUtils.tryCommit(session.localEventLoop(), rpcResponseCommitTask);
        }
    }


    /**
     * 清理Rpc请求信息
     *
     * @param session           会话信息
     * @param rpcPromiseInfoMap 未完成的Rpc请求
     */
    protected final void cleanRpcPromiseInfo(final Session session, final Long2ObjectMap<RpcPromiseInfo> rpcPromiseInfoMap) {
        if (rpcPromiseInfoMap.size() == 0) {
            return;
        }
        // 立即通知所有rpcPromise - 因为用户可能阻塞在上面。
        CollectionUtils.removeIfAndThen(rpcPromiseInfoMap.values(),
                rpcPromiseInfo -> rpcPromiseInfo.rpcPromise != null,
                rpcPromiseInfo -> rpcPromiseInfo.rpcPromise.trySuccess(RpcResponse.SESSION_CLOSED));
        // 减少不必要的提交
        if (rpcPromiseInfoMap.size() == 0) {
            return;
        }
        // 异步rpc回调，需要提交到用户线程才能执行。
        // 这里批量提交的影响较小，因此选择批量提交
        ConcurrentUtils.tryCommit(session.localEventLoop(), () -> {
            for (RpcPromiseInfo rpcPromiseInfo : rpcPromiseInfoMap.values()) {
                ConcurrentUtils.safeExecute((Runnable) () -> rpcPromiseInfo.rpcCallback.onComplete(RpcResponse.SESSION_CLOSED));
            }
        });
    }

    // --------------------------------------------------- 各种内部封装 -------------------------------------------

    /**
     * Session的封装对象。
     *
     * @author wjybxx
     * @version 1.0
     * date - 2019/9/11
     * github - https://github.com/hl845740757
     */
    public abstract static class ISessionWrapper<T extends Session> {

        /**
         * 关联的session
         */
        protected final T session;

        /**
         * RpcRequestId分配器
         */
        private long requestGuidSequencer = 0;

        /**
         * 当前会话上的rpc请求
         * (提供顺序保证，先发起的请求先超时)
         */
        private Long2ObjectMap<RpcPromiseInfo> rpcPromiseInfoMap = new Long2ObjectLinkedOpenHashMap<>();

        protected ISessionWrapper(T session) {
            this.session = session;
        }


        public final T getSession() {
            return session;
        }

        public final long nextRequestGuid() {
            return ++requestGuidSequencer;
        }

        /**
         * 删除rpcPromiseInfoMap并返回
         */
        public final Long2ObjectMap<RpcPromiseInfo> detachRpcPromiseInfoMap() {
            Long2ObjectMap<RpcPromiseInfo> result = rpcPromiseInfoMap;
            rpcPromiseInfoMap = null;
            return result;
        }

        public Long2ObjectMap<RpcPromiseInfo> getRpcPromiseInfoMap() {
            return rpcPromiseInfoMap;
        }

        public abstract void sendOneWayMessage(@Nonnull Object message, boolean immediate);

        public abstract void sendRpcRequest(long requestGuid, boolean immediate, @Nonnull Object request);

        public abstract void sendRpcResponse(long requestGuid, boolean immediate, @Nonnull RpcResponse response);

    }
}
