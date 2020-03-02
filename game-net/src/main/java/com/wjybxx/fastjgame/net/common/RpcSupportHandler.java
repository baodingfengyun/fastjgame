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

package com.wjybxx.fastjgame.net.common;

import com.wjybxx.fastjgame.net.exception.DefaultRpcServerException;
import com.wjybxx.fastjgame.net.exception.RpcSessionClosedException;
import com.wjybxx.fastjgame.net.exception.RpcTimeoutException;
import com.wjybxx.fastjgame.net.session.Session;
import com.wjybxx.fastjgame.net.session.SessionDuplexHandlerAdapter;
import com.wjybxx.fastjgame.net.session.SessionHandlerContext;
import com.wjybxx.fastjgame.net.task.RpcRequestCommitTask;
import com.wjybxx.fastjgame.net.task.RpcRequestWriteTask;
import com.wjybxx.fastjgame.net.task.RpcResponseWriteTask;
import com.wjybxx.fastjgame.utils.CollectionUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.IOException;

/**
 * 提供Rpc调用支持的handler。
 * 收发{@link RpcRequestMessage}
 * <p>
 * 实现需要注意：
 * 1.rpc响应在应用线程未关闭的情况下必须执行 - 否则可能造成逻辑错误(信号丢失 - 该执行的没执行)
 * 2.目前的rpc支持为{@link RpcRequest} 和 {@link RpcResponse}
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/9/26
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public class RpcSupportHandler extends SessionDuplexHandlerAdapter {

    private long rpcCallbackTimeoutMs;
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
    public void handlerAdded(SessionHandlerContext ctx) throws Exception {
        rpcCallbackTimeoutMs = ctx.session().config().getRpcCallbackTimeoutMs();
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
            long deadline = ctx.timerSystem().curTimeMillis() + rpcCallbackTimeoutMs;
            RpcTimeoutInfo rpcTimeoutInfo = new RpcTimeoutInfo(writeTask.getRpcPromise(), deadline);
            long requestGuid = ++requestGuidSequencer;
            rpcTimeoutInfoMap.put(requestGuid, rpcTimeoutInfo);

            // 发送
            ctx.fireWrite(new RpcRequestMessage(requestGuid, writeTask.isSync(), writeTask.getRequest()));
        } else if (msg instanceof RpcResponseWriteTask) {
            // rpc调用结果
            RpcResponseWriteTask writeTask = (RpcResponseWriteTask) msg;
            final RpcResponseMessage responseMessage = new RpcResponseMessage(writeTask.getRequestGuid(), writeTask.getResponse());

            // 发送
            ctx.fireWrite(responseMessage);
        } else {
            ctx.fireWrite(msg);
        }
    }

    @Override
    public void read(SessionHandlerContext ctx, Object msg) throws IOException {
        if (msg instanceof RpcRequestMessage) {
            // 读取到一个Rpc请求消息，提交给应用层
            RpcRequestMessage requestMessage = (RpcRequestMessage) msg;
            RpcResponseChannel<?> rpcResponseChannel = new DefaultRpcResponseChannel<>(ctx.session(),
                    requestMessage.getRequestGuid(), requestMessage.isSync());

            // 检查提前反序列化字段
            ctx.appEventLoop().execute(new RpcRequestCommitTask(ctx.session(), requestMessage.getBody(), rpcResponseChannel));
        } else if (msg instanceof RpcResponseMessage) {
            // 读取到一个Rpc响应消息，提交给应用层
            RpcResponseMessage responseMessage = (RpcResponseMessage) msg;
            final RpcTimeoutInfo rpcTimeoutInfo = rpcTimeoutInfoMap.remove(responseMessage.getRequestGuid());
            if (null != rpcTimeoutInfo) {
                commitRpcResponse(rpcTimeoutInfo, (RpcResponse) responseMessage.getBody());
            }
            // else 可能超时了
        } else {
            ctx.fireRead(msg);
        }
    }

    /**
     * @param rpcTimeoutInfo rpc请求时的一些信息
     * @param rpcResponse    期望提交的rpc调用结果。
     */
    @SuppressWarnings("unchecked")
    private <V> void commitRpcResponse(RpcTimeoutInfo rpcTimeoutInfo, RpcResponse rpcResponse) {
        final V result;
        final Throwable cause;
        if (rpcResponse.isSuccess()) {
            result = (V) rpcResponse.getBody();
            cause = null;
        } else {
            result = null;
            cause = new DefaultRpcServerException(rpcResponse);
        }

        if (cause != null) {
            rpcTimeoutInfo.rpcPromise.tryFailure(cause);
        } else {
            final RpcPromise<V> rpcPromise = (RpcPromise<V>) rpcTimeoutInfo.rpcPromise;
            rpcPromise.trySuccess(result);
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
            if (!session.isClosed()) {
                session.netEventLoop().execute(new RpcResponseWriteTask(session, requestGuid, sync, rpcResponse));
            }
        }
    }

    private static class RpcTimeoutInfo {

        final RpcPromise<?> rpcPromise;
        final long deadline;

        RpcTimeoutInfo(RpcPromise<?> rpcPromise, long deadline) {
            this.rpcPromise = rpcPromise;
            this.deadline = deadline;
        }
    }

}
