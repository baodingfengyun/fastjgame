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

package com.wjybxx.fastjgame.net.handler;

import com.wjybxx.fastjgame.concurrent.Promise;
import com.wjybxx.fastjgame.net.*;
import com.wjybxx.fastjgame.net.pipeline.SessionDuplexHandlerAdapter;
import com.wjybxx.fastjgame.net.pipeline.SessionHandlerContext;
import com.wjybxx.fastjgame.utils.CollectionUtils;
import com.wjybxx.fastjgame.utils.ConcurrentUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * 提供Rpc调用支持的handler。
 * 收发{@link RpcRequestMessage}
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/9/26
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public class RpcSupportHandler extends SessionDuplexHandlerAdapter {

    /**
     * RpcRequestId分配器
     */
    private long requestGuidSequencer = 0;

    /**
     * 当前会话上的rpc请求
     * (提供顺序保证，先发起的请求先超时)
     */
    private Long2ObjectMap<RpcTimeoutInfo> rpcPromiseInfoMap = new Long2ObjectLinkedOpenHashMap<>();

    private final ProtocolDispatcher protocolDispatcher;

    public RpcSupportHandler(ProtocolDispatcher protocolDispatcher) {
        this.protocolDispatcher = protocolDispatcher;
    }

    @Override
    public void tick(SessionHandlerContext ctx) {
        if (rpcPromiseInfoMap.size() == 0) {
            return;
        }
        long systemMillTime = ctx.managerWrapper().getNetTimeManager().getSystemMillTime();
        ObjectIterator<RpcTimeoutInfo> iterator = rpcPromiseInfoMap.values().iterator();
        while (iterator.hasNext()) {
            RpcTimeoutInfo rpcTimeoutInfo = iterator.next();
            if (systemMillTime >= rpcTimeoutInfo.deadline) {
                iterator.remove();
                commitRpcResponse(ctx.session(), rpcTimeoutInfo, RpcResponse.TIMEOUT);
            }
        }
    }

    private void commitRpcResponse(Session session, RpcTimeoutInfo rpcTimeoutInfo, RpcResponse rpcResponse) {
        if (rpcTimeoutInfo.rpcPromise != null) {
            // 同步rpc调用
            if (session.isActive()) {
                rpcTimeoutInfo.rpcPromise.trySuccess(rpcResponse);
            } else {
                rpcTimeoutInfo.rpcPromise.trySuccess(RpcResponse.SESSION_CLOSED);
            }
        } else {
            // 异步rpc调用
            RpcResponseCommitTask rpcResponseCommitTask;
            if (session.isActive()) {
                rpcResponseCommitTask = new RpcResponseCommitTask(rpcTimeoutInfo.rpcCallback, rpcResponse);
            } else {
                rpcResponseCommitTask = new RpcResponseCommitTask(rpcTimeoutInfo.rpcCallback, RpcResponse.SESSION_CLOSED);
            }
            ConcurrentUtils.tryCommit(session.localEventLoop(), rpcResponseCommitTask);
        }
    }

    @Override
    public void write(SessionHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof AsyncRpcRequestWriteTask) {
            // 异步rpc请求
            AsyncRpcRequestWriteTask writeTask = (AsyncRpcRequestWriteTask) msg;

            // 保存rpc请求上下文
            long deadline = ctx.managerWrapper().getNetTimeManager().getSystemMillTime() + writeTask.getTimeoutMs();
            RpcTimeoutInfo rpcTimeoutInfo = RpcTimeoutInfo.newInstance(writeTask.getRpcCallback(), deadline);
            long requestGuid = ++requestGuidSequencer;
            rpcPromiseInfoMap.put(requestGuid, rpcTimeoutInfo);

            // 执行发送
            ctx.write(new RpcRequestMessage(requestGuid, false, writeTask.getRequest()));
        } else if (msg instanceof SyncRpcRequestWriteTask) {
            // 同步rpc请求
            SyncRpcRequestWriteTask writeTask = (SyncRpcRequestWriteTask) msg;

            // 保存rpc请求上下文
            RpcTimeoutInfo rpcTimeoutInfo = RpcTimeoutInfo.newInstance(writeTask.getRpcPromise(), writeTask.getRpcPromise().deadline());
            long requestGuid = ++requestGuidSequencer;
            rpcPromiseInfoMap.put(requestGuid, rpcTimeoutInfo);

            ctx.write(new RpcRequestMessage(requestGuid, true, writeTask.getRequest()));
            // 同步调用，需要刷新缓冲区
            ctx.flush();

        } else if (msg instanceof RpcResponseWriteTask) {
            // rpc结果任务
            RpcResponseWriteTask writeTask = (RpcResponseWriteTask) msg;
            ctx.write(new RpcResponseMessage(writeTask.getRequestGuid(), writeTask.getResponse()));
            // 同步调用的结果，需要刷新缓冲区，尽快的返回结果
            if (writeTask.isSync()) {
                ctx.flush();
            }
        } else {
            ctx.write(msg);
        }
    }

    @Override
    public void read(SessionHandlerContext ctx, Object msg) {
        if (msg instanceof RpcRequestMessage) {
            // 读取到一个Rpc请求消息，提交给应用层
            RpcRequestMessage requestMessage = (RpcRequestMessage) msg;
            DefaultRpcResponseChannel<?> rpcResponseChannel = new DefaultRpcResponseChannel(ctx.session(),
                    requestMessage.getRequestGuid(), requestMessage.isSync());

            ConcurrentUtils.tryCommit(ctx.localEventLoop(),
                    new RpcRequestCommitTask(ctx.session(), protocolDispatcher, rpcResponseChannel, requestMessage.getRequest()));
        } else if (msg instanceof RpcResponseMessage) {
            // 读取到一个Rpc响应消息，提交给应用层
            RpcResponseMessage responseMessage = (RpcResponseMessage) msg;
            final RpcTimeoutInfo rpcTimeoutInfo = rpcPromiseInfoMap.remove(responseMessage.getRequestGuid());
            if (null != rpcTimeoutInfo) {
                commitRpcResponse(ctx.session(), rpcTimeoutInfo, responseMessage.getRpcResponse());
            }
            // else 可能超时了
        } else {
            ctx.fireRead(msg);
        }
    }

    @Override
    public void close(SessionHandlerContext ctx, Promise<?> promise) throws Exception {
        try {
            cleanRpcPromiseInfo(ctx);
        } finally {
            ctx.close(promise);
        }
    }

    private void cleanRpcPromiseInfo(SessionHandlerContext ctx) {
        if (rpcPromiseInfoMap.size() == 0) {
            return;
        }
        // 立即通知所有rpcPromise - 因为用户可能阻塞在上面。
        CollectionUtils.removeIfAndThen(rpcPromiseInfoMap.values(),
                rpcTimeoutInfo -> rpcTimeoutInfo.rpcPromise != null,
                rpcTimeoutInfo -> rpcTimeoutInfo.rpcPromise.trySuccess(RpcResponse.SESSION_CLOSED));
        // 减少不必要的提交
        if (rpcPromiseInfoMap.size() == 0) {
            return;
        }
        // 异步rpc回调，需要提交到用户线程才能执行。
        // 这里批量提交的影响较小，因此选择批量提交
        ConcurrentUtils.tryCommit(ctx.localEventLoop(), () -> {
            for (RpcTimeoutInfo rpcTimeoutInfo : rpcPromiseInfoMap.values()) {
                ConcurrentUtils.safeExecute((Runnable) () -> rpcTimeoutInfo.rpcCallback.onComplete(RpcResponse.SESSION_CLOSED));
            }
        });
    }

    private static class DefaultRpcResponseChannel<T> extends AbstractRpcResponseChannel<T> {

        private final Session session;
        private final long rpcRequestGuid;
        private final boolean sync;

        private DefaultRpcResponseChannel(Session session, long rpcRequestGuid, boolean sync) {
            this.session = session;
            this.rpcRequestGuid = rpcRequestGuid;
            this.sync = sync;
        }

        @Override
        protected void doWrite(RpcResponse rpcResponse) {
            session.write(new RpcResponseWriteTask(session, rpcRequestGuid, sync, rpcResponse));
        }
    }
}
