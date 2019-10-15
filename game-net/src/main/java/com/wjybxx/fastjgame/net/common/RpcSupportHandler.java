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

import com.wjybxx.fastjgame.manager.NetTimeManager;
import com.wjybxx.fastjgame.net.session.Session;
import com.wjybxx.fastjgame.net.session.SessionDuplexHandlerAdapter;
import com.wjybxx.fastjgame.net.session.SessionHandlerContext;
import com.wjybxx.fastjgame.net.task.*;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
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

    private NetTimeManager netTimeManager;
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
        netTimeManager = ctx.managerWrapper().getNetTimeManager();
        rpcCallbackTimeoutMs = ctx.session().config().getRpcCallbackTimeoutMs();
    }

    @Override
    public void tick(SessionHandlerContext ctx) {
        if (rpcTimeoutInfoMap.size() == 0) {
            return;
        }
        long systemMillTime = netTimeManager.getSystemMillTime();
        ObjectIterator<RpcTimeoutInfo> iterator = rpcTimeoutInfoMap.values().iterator();
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
     * @param rpcResponse    期望提交的rpc调用结果。
     */
    private void commitRpcResponse(Session session, RpcTimeoutInfo rpcTimeoutInfo, RpcResponse rpcResponse) {
        if (rpcTimeoutInfo.rpcPromise != null) {
            // 同步rpc调用
            rpcTimeoutInfo.rpcPromise.trySuccess(rpcResponse);
        } else {
            // 异步rpc调用
            session.localEventLoop().execute(new RpcResponseCommitTask(session, rpcTimeoutInfo.rpcCallback, rpcResponse));
        }
    }

    @Override
    public void onSessionInactive(SessionHandlerContext ctx) throws Exception {
        try {
            cancelAllRpcRequest(ctx);
        } finally {
            ctx.fireSessionInactive();
        }
    }

    @Override
    public void write(SessionHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof AsyncRpcRequestWriteTask) {
            // 异步rpc请求
            AsyncRpcRequestWriteTask writeTask = (AsyncRpcRequestWriteTask) msg;

            // 保存rpc请求上下文
            long deadline = netTimeManager.getSystemMillTime() + rpcCallbackTimeoutMs;
            RpcTimeoutInfo rpcTimeoutInfo = RpcTimeoutInfo.newInstance(writeTask.getRpcCallback(), deadline);
            long requestGuid = ++requestGuidSequencer;
            rpcTimeoutInfoMap.put(requestGuid, rpcTimeoutInfo);

            // 执行发送
            ctx.fireWrite(new RpcRequestMessage(requestGuid, false, writeTask.getRequest()));
        } else if (msg instanceof SyncRpcRequestWriteTask) {
            // 同步rpc请求
            SyncRpcRequestWriteTask writeTask = (SyncRpcRequestWriteTask) msg;

            // 保存rpc请求上下文
            RpcTimeoutInfo rpcTimeoutInfo = RpcTimeoutInfo.newInstance(writeTask.getRpcPromise(), writeTask.getRpcPromise().deadline());
            long requestGuid = ++requestGuidSequencer;
            rpcTimeoutInfoMap.put(requestGuid, rpcTimeoutInfo);

            // 执行发送
            ctx.fireWrite(new RpcRequestMessage(requestGuid, true, writeTask.getRequest()));
        } else if (msg instanceof RpcResponseWriteTask) {
            // rpc调用结果
            RpcResponseWriteTask writeTask = (RpcResponseWriteTask) msg;
            final RpcResponseMessage responseMessage = new RpcResponseMessage(writeTask.getRequestGuid(), writeTask.getResponse());

            // 执行发送
            ctx.fireWrite(responseMessage);
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

            ctx.localEventLoop().execute(new RpcRequestCommitTask(ctx.session(), rpcResponseChannel, requestMessage.getRequest()));
        } else if (msg instanceof RpcResponseMessage) {
            // 读取到一个Rpc响应消息，提交给应用层
            RpcResponseMessage responseMessage = (RpcResponseMessage) msg;
            final RpcTimeoutInfo rpcTimeoutInfo = rpcTimeoutInfoMap.remove(responseMessage.getRequestGuid());
            if (null != rpcTimeoutInfo) {
                commitRpcResponse(ctx.session(), rpcTimeoutInfo, responseMessage.getRpcResponse());
            }
            // else 可能超时了
        } else {
            ctx.fireRead(msg);
        }
    }

    /**
     * 取消所有的rpc请求
     */
    private void cancelAllRpcRequest(SessionHandlerContext ctx) {
        for (RpcTimeoutInfo rpcTimeoutInfo : rpcTimeoutInfoMap.values()) {
            commitRpcResponse(ctx.session(), rpcTimeoutInfo, RpcResponse.SESSION_CLOSED);
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
            if (session.isActive()) {
                session.netEventLoop().execute(new RpcResponseWriteTask(session, requestGuid, sync, rpcResponse));
            }
        }
    }
}
