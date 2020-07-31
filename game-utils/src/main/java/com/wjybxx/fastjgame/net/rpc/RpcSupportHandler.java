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

package com.wjybxx.fastjgame.net.rpc;

import com.wjybxx.fastjgame.net.exception.DefaultRpcServerException;
import com.wjybxx.fastjgame.net.exception.RpcSessionClosedException;
import com.wjybxx.fastjgame.net.exception.RpcTimeoutException;
import com.wjybxx.fastjgame.net.session.Session;
import com.wjybxx.fastjgame.net.session.SessionDuplexHandlerAdapter;
import com.wjybxx.fastjgame.net.session.SessionHandlerContext;
import com.wjybxx.fastjgame.utils.concurrent.FutureListener;
import com.wjybxx.fastjgame.utils.concurrent.FutureUtils;
import com.wjybxx.fastjgame.utils.concurrent.ListenableFuture;
import com.wjybxx.fastjgame.utils.concurrent.Promise;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import java.io.IOException;

/**
 * 提供Rpc调用支持的handler。
 * 收发{@link RpcRequestMessage}
 * <p>
 * 实现需要注意：
 * 1.rpc响应在应用线程未关闭的情况下必须执行 - 否则可能造成逻辑错误(信号丢失 - 该执行的没执行)
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
     * rpc请求超时信息
     * - 在现在的设计中，只有服务器之间有rpc支持，与玩家之间是没有该handler的，因此不会浪费资源。
     * - 避免频繁的扩容，扩容和重新计算hash值是非常消耗资源的。
     */
    private final Long2ObjectLinkedOpenHashMap<RpcTimeoutInfo> rpcTimeoutInfoMap = new Long2ObjectLinkedOpenHashMap<>(1024);

    public RpcSupportHandler() {

    }

    @Override
    public void tick(SessionHandlerContext ctx) {
        checkRpcTimeout(ctx);
    }

    /**
     * 并不需要检查全部的rpc请求，只要第一个未超时，即可停止。
     * Q: why?
     * A: 当第一个是同步rpc调用时，检查异步rpc的超时没有太大意义，因为用户线程是阻塞的。
     * 当第一个是异步rpc调用时，如果该rpc请求未超时，那么它后面的rpc请求也没超时，因为异步rpc请求之间是有序的，先请求的先超时，后请求的后超时。
     */
    private void checkRpcTimeout(SessionHandlerContext ctx) {
        final long curTimeMillis = ctx.timerSystem().curTimeMillis();
        while (rpcTimeoutInfoMap.size() > 0) {
            final long requestGuid = rpcTimeoutInfoMap.firstLongKey();
            final RpcTimeoutInfo timeoutInfo = rpcTimeoutInfoMap.get(requestGuid);

            if (curTimeMillis < timeoutInfo.deadline) {
                return;
            }

            rpcTimeoutInfoMap.removeFirst();
            timeoutInfo.rpcPromise.tryFailure(RpcTimeoutException.INSTANCE);
        }
    }

    @Override
    public void onSessionInactive(SessionHandlerContext ctx) throws Exception {
        try {
            cancelAllRpcRequest();
        } finally {
            ctx.fireSessionInactive();
        }
    }

    @Override
    public void write(SessionHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof RpcRequestWriteTask) {
            // rpc请求
            RpcRequestWriteTask writeTask = (RpcRequestWriteTask) msg;

            long deadline = ctx.timerSystem().curTimeMillis() + writeTask.getTimeoutMs();
            RpcTimeoutInfo rpcTimeoutInfo = new RpcTimeoutInfo(writeTask.getPromise(), deadline);
            long requestGuid = ++requestGuidSequencer;

            // 保存超时信息，如果是同步rpc调用，放在超时信息的首位
            if (writeTask.isSync()) {
                rpcTimeoutInfoMap.putAndMoveToFirst(requestGuid, rpcTimeoutInfo);
            } else {
                rpcTimeoutInfoMap.put(requestGuid, rpcTimeoutInfo);
            }

            ctx.fireWrite(new RpcRequestMessage(requestGuid, writeTask.isSync(), writeTask.getRequest()));
        } else {
            ctx.fireWrite(msg);
        }
    }

    @Override
    public void read(SessionHandlerContext ctx, Object msg) throws IOException {
        if (msg instanceof RpcRequestMessage) {
            // 读取到一个Rpc请求消息，提交给应用层
            final RpcRequestMessage requestMessage = (RpcRequestMessage) msg;
            // 创建执行上下文
            final DefaultRpcProcessContext context = new DefaultRpcProcessContext(ctx.session(), requestMessage.getRequestGuid(), requestMessage.isSync());
            final Promise<?> promise = FutureUtils.newPromise();

            ctx.appEventLoop().execute(new RpcRequestCommitTask(context, requestMessage.getBody(), promise));

            promise.addListener(new RpcResultListener(context), ctx.netEventLoop());
        } else if (msg instanceof RpcResponseMessage) {
            // 读取到一个Rpc响应消息，提交给应用层
            final RpcResponseMessage responseMessage = (RpcResponseMessage) msg;
            final long requestGuid = responseMessage.getRequestGuid();

            final RpcTimeoutInfo rpcTimeoutInfo = rpcTimeoutInfoMap.remove(requestGuid);
            if (null != rpcTimeoutInfo) {
                commitRpcResponse(rpcTimeoutInfo.rpcPromise, responseMessage.getErrorCode(), responseMessage.getBody());
            }
            // else 可能超时了
        } else {
            ctx.fireRead(msg);
        }
    }

    /**
     * @param rpcPromise 接收结果的promise
     * @param body       期望提交的rpc调用结果。
     */
    private <V> void commitRpcResponse(Promise<V> rpcPromise, RpcErrorCode errorCode, Object body) {
        if (errorCode.isSuccess()) {
            @SuppressWarnings("unchecked") final V result = (V) body;
            rpcPromise.trySuccess(result);
        } else {
            final Throwable cause = new DefaultRpcServerException(errorCode, String.valueOf(body));
            rpcPromise.tryFailure(cause);
        }
    }

    /**
     * 取消所有的rpc请求
     */
    private void cancelAllRpcRequest() {
        for (RpcTimeoutInfo rpcTimeoutInfo : rpcTimeoutInfoMap.values()) {
            rpcTimeoutInfo.rpcPromise.tryFailure(RpcSessionClosedException.INSTANCE);
        }
        rpcTimeoutInfoMap.clear();
    }

    private static class RpcTimeoutInfo {

        private final Promise<?> rpcPromise;
        private final long deadline;

        RpcTimeoutInfo(Promise<?> rpcPromise, long deadline) {
            this.rpcPromise = rpcPromise;
            this.deadline = deadline;
        }
    }

    private static class DefaultRpcProcessContext implements RpcProcessContext {

        private final Session session;
        private final long requestGuid;
        private final boolean sync;

        DefaultRpcProcessContext(Session session, long requestGuid, boolean sync) {
            this.session = session;
            this.requestGuid = requestGuid;
            this.sync = sync;
        }

        @Nonnull
        @Override
        public Session session() {
            return session;
        }

        @Override
        public boolean isRpc() {
            return true;
        }

        @Override
        public long requestGuid() {
            return requestGuid;
        }

        @Override
        public boolean isSyncRpc() {
            return sync;
        }
    }

    private static class RpcResultListener implements FutureListener<Object> {

        private final DefaultRpcProcessContext context;

        RpcResultListener(DefaultRpcProcessContext context) {
            this.context = context;
        }

        @Override
        public void onComplete(ListenableFuture<Object> future) throws Exception {
            if (context.session.isClosed()) {
                return;
            }

            final RpcErrorCode errorCode;
            final Object body;
            if (future.isCompletedExceptionally()) {
                errorCode = RpcErrorCode.SERVER_EXCEPTION;
                // 不返回完整信息，意义不大
                body = ExceptionUtils.getRootCauseMessage(future.cause());
            } else {
                errorCode = RpcErrorCode.SUCCESS;
                body = future.getNow();
            }

            // 此时已经在网络线程，直接write,但还是需要流经整个管道
            final RpcResponseMessage responseMessage = new RpcResponseMessage(context.requestGuid, context.sync, errorCode, body);
            context.session.fireWrite(responseMessage);
        }
    }

}
