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
import com.wjybxx.fastjgame.utils.CollectionUtils;
import com.wjybxx.fastjgame.utils.DebugUtils;
import com.wjybxx.fastjgame.utils.concurrent.FutureListener;
import com.wjybxx.fastjgame.utils.concurrent.ListenableFuture;
import com.wjybxx.fastjgame.utils.concurrent.LocalPromise;
import com.wjybxx.fastjgame.utils.concurrent.Promise;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
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
     * 当前会话上的rpc请求。
     * - 在现在的设计中，只有服务器之间有rpc支持，与玩家之间是没有该handler的，因此不会浪费资源。
     * - 避免频繁的扩容，扩容和重新计算hash值是非常消耗资源的。
     */
    private final Long2ObjectMap<RpcTimeoutInfo> rpcTimeoutInfoMap = new Long2ObjectOpenHashMap<>(1024);

    public RpcSupportHandler() {

    }

    @Override
    public void tick(SessionHandlerContext ctx) {
        if (rpcTimeoutInfoMap.size() == 0) {
            return;
        }

        final long curTimeMillis = ctx.timerSystem().curTimeMillis();
        CollectionUtils.removeIfAndThen(rpcTimeoutInfoMap.values(),
                rpcTimeoutInfo -> curTimeMillis >= rpcTimeoutInfo.deadline,
                rpcTimeoutInfo -> rpcTimeoutInfo.rpcPromise.tryFailure(RpcTimeoutException.INSTANCE));
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

            // 保存rpc请求上下文
            long deadline = ctx.timerSystem().curTimeMillis() + writeTask.getTimeoutMs();
            RpcTimeoutInfo rpcTimeoutInfo = new RpcTimeoutInfo(writeTask.getPromise(), deadline);
            long requestGuid = ++requestGuidSequencer;
            rpcTimeoutInfoMap.put(requestGuid, rpcTimeoutInfo);

            ctx.fireWrite(new RpcRequestMessage(requestGuid, writeTask.isSync(), writeTask.getRequest()));
        } else if (msg instanceof RpcResponseWriteTask) {
            // rpc调用结果
            RpcResponseWriteTask writeTask = (RpcResponseWriteTask) msg;

            ctx.fireWrite(writeTask.getRpcResponseMessage());
        } else {
            ctx.fireWrite(msg);
        }
    }

    @Override
    public void read(SessionHandlerContext ctx, Object msg) throws IOException {
        if (msg instanceof RpcRequestMessage) {
            // 读取到一个Rpc请求消息，提交给应用层
            final RpcRequestMessage requestMessage = (RpcRequestMessage) msg;
            // 这里网络层是监听器的用户，创建LocalPromise没错的
            final LocalPromise<?> promise = ctx.netEventLoop().newLocalPromise();
            promise.onComplete(new RpcResultListener(ctx.session(), requestMessage.getRequestGuid(), requestMessage.isSync()));

            // 提交给用户执行
            ctx.appEventLoop().execute(new RpcRequestCommitTask(ctx.session(), requestMessage.getBody(), promise));
        } else if (msg instanceof RpcResponseMessage) {
            // 读取到一个Rpc响应消息，提交给应用层
            RpcResponseMessage responseMessage = (RpcResponseMessage) msg;
            final RpcTimeoutInfo rpcTimeoutInfo = rpcTimeoutInfoMap.remove(responseMessage.getRequestGuid());
            if (null != rpcTimeoutInfo) {
                commitRpcResponse(rpcTimeoutInfo, responseMessage.getErrorCode(), responseMessage.getBody());
            }
            // else 可能超时了
        } else {
            ctx.fireRead(msg);
        }
    }

    /**
     * @param rpcTimeoutInfo rpc请求时的一些信息
     * @param body           期望提交的rpc调用结果。
     */
    @SuppressWarnings("unchecked")
    private <V> void commitRpcResponse(RpcTimeoutInfo rpcTimeoutInfo, RpcErrorCode errorCode, Object body) {
        if (errorCode.isSuccess()) {
            final Promise<V> rpcPromise = (Promise<V>) rpcTimeoutInfo.rpcPromise;
            final V result = (V) body;
            rpcPromise.trySuccess(result);
        } else {
            final Throwable cause = new DefaultRpcServerException(errorCode, String.valueOf(body));
            rpcTimeoutInfo.rpcPromise.tryFailure(cause);
        }
    }

    /**
     * 取消所有的rpc请求
     */
    private void cancelAllRpcRequest() {
        for (RpcTimeoutInfo rpcTimeoutInfo : rpcTimeoutInfoMap.values()) {
            rpcTimeoutInfo.rpcPromise.tryFailure(RpcSessionClosedException.INSTANCE);
        }
    }

    private static class RpcTimeoutInfo {

        final Promise<?> rpcPromise;
        final long deadline;

        RpcTimeoutInfo(Promise<?> rpcPromise, long deadline) {
            this.rpcPromise = rpcPromise;
            this.deadline = deadline;
        }
    }

    private static class RpcResultListener implements FutureListener<Object> {
        private final Session session;
        private final long requestGuid;
        private final boolean sync;

        private RpcResultListener(Session session, long requestGuid, boolean sync) {
            this.session = session;
            this.requestGuid = requestGuid;
            this.sync = sync;
        }

        @Override
        public void onComplete(ListenableFuture<Object> future) throws Exception {
            if (session.isClosed()) {
                return;
            }

            final RpcErrorCode errorCode;
            final Object body;
            if (future.isSuccess()) {
                errorCode = RpcErrorCode.SUCCESS;
                body = future.getNow();
            } else {
                errorCode = RpcErrorCode.SERVER_EXCEPTION;
                body = getCauseMessage(future.cause());
            }

            session.netEventLoop().execute(new RpcResponseWriteTask(session, requestGuid, errorCode, body, sync));
        }
    }

    private static String getCauseMessage(@Nonnull Throwable cause) {
        final String message;
        if (DebugUtils.isDebugOpen()) {
            // debug开启情况下，返回详细信息
            message = ExceptionUtils.getStackTrace(cause);
        } else {
            message = ExceptionUtils.getRootCauseMessage(cause);
        }
        return message;
    }
}
