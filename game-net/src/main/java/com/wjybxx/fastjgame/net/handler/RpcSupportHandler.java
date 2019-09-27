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

    /**
     * rpc回调必须执行 - 否则可能造成逻辑错误(信号丢失 - 该执行的没执行)
     *
     * @param rpcTimeoutInfo rpc请求时的一些信息
     * @param rcvRpcResponse 期望提交的rpc调用结果。
     */
    private void commitRpcResponse(Session session, RpcTimeoutInfo rpcTimeoutInfo, RpcResponse rcvRpcResponse) {
        RpcResponse rpcResponse = session.isActive() ? rcvRpcResponse : RpcResponse.SESSION_CLOSED;
        if (rpcTimeoutInfo.rpcPromise != null) {
            // 同步rpc调用
            rpcTimeoutInfo.rpcPromise.trySuccess(rpcResponse);
        } else {
            // 异步rpc调用
            ConcurrentUtils.tryCommit(session.localEventLoop(), new RpcResponseCommitTask(rpcTimeoutInfo.rpcCallback, rpcResponse));
        }
    }

    @Override
    public void write(SessionHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof AsyncRpcRequestWriteTask) {
            // 异步rpc请求
            AsyncRpcRequestWriteTask writeTask = (AsyncRpcRequestWriteTask) msg;

            // 保存rpc请求上下文
            long deadline = ctx.managerWrapper().getNetTimeManager().getSystemMillTime() + ctx.session().config().getRpcCallbackTimeoutMs();
            RpcTimeoutInfo rpcTimeoutInfo = RpcTimeoutInfo.newInstance(writeTask.getRpcCallback(), deadline);
            long requestGuid = ++requestGuidSequencer;
            rpcPromiseInfoMap.put(requestGuid, rpcTimeoutInfo);

            // 执行发送
            ctx.fireWrite(new RpcRequestMessage(requestGuid, false, writeTask.getRequest()));
        } else if (msg instanceof SyncRpcRequestWriteTask) {
            // 同步rpc请求
            SyncRpcRequestWriteTask writeTask = (SyncRpcRequestWriteTask) msg;

            // 保存rpc请求上下文
            RpcTimeoutInfo rpcTimeoutInfo = RpcTimeoutInfo.newInstance(writeTask.getRpcPromise(), writeTask.getRpcPromise().deadline());
            long requestGuid = ++requestGuidSequencer;
            rpcPromiseInfoMap.put(requestGuid, rpcTimeoutInfo);

            // 执行发送 - 并清空缓冲区
            ctx.fireWriteAndFlush(new RpcRequestMessage(requestGuid, true, writeTask.getRequest()));
        } else if (msg instanceof RpcResponseWriteTask) {
            // rpc调用结果
            RpcResponseWriteTask writeTask = (RpcResponseWriteTask) msg;
            final RpcResponseMessage responseMessage = new RpcResponseMessage(writeTask.getRequestGuid(), writeTask.getResponse());

            // 同步调用的结果，需要刷新缓冲区，尽快的返回结果，异步的则无需着急刷新缓冲区
            if (writeTask.isSync()) {
                ctx.fireWriteAndFlush(responseMessage);
            } else {
                ctx.fireWrite(responseMessage);
            }
        } else {
            ctx.fireWrite(msg);
        }
    }

    @Override
    public void read(SessionHandlerContext ctx, Object msg) {
        if (msg instanceof RpcRequestMessage) {
            // 读取到一个Rpc请求消息，提交给应用层
            RpcRequestMessage requestMessage = (RpcRequestMessage) msg;
            RpcResponseChannel<?> rpcResponseChannel = new DefaultRpcResponseChannel<>(ctx.session(),
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
            cancelAllRpcRequest(ctx);
        } finally {
            ctx.fireClose(promise);
        }
    }

    private void cancelAllRpcRequest(SessionHandlerContext ctx) {
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
        private final long requestGuid;
        private final boolean sync;

        private DefaultRpcResponseChannel(Session session, long requestGuid, boolean sync) {
            this.session = session;
            this.requestGuid = requestGuid;
            this.sync = sync;
        }

        @Override
        protected void doWrite(RpcResponse rpcResponse) {
            if (session.isActive()) {
                session.netEventLoop().execute(new RpcResponseWriteTask(session, requestGuid, sync, rpcResponse));
            }
        }
    }
}
