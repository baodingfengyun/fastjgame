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
import com.wjybxx.fastjgame.net.session.SessionConfig;
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
import java.util.ArrayList;
import java.util.List;

/**
 * 提供Rpc调用支持的handler。
 * 收发{@link RpcRequestMessage}
 * <p>
 * 实现需要注意：
 * 1.rpc响应在应用线程未关闭的情况下必须执行 - 否则可能造成逻辑错误(信号丢失 - 该执行的没执行)
 * 2.目前的rpc支持为{@link RpcCall} 和 {@link RpcResponse}
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/9/26
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public class RpcSupportHandler extends SessionDuplexHandlerAdapter {

    private ProtocolCodec codec;
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
        final SessionConfig config = ctx.session().config();
        rpcCallbackTimeoutMs = config.getRpcCallbackTimeoutMs();
        codec = config.codec();
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

            // 检查延迟序列化字段
            final Object body = checkLazySerialize(writeTask.getRequest());
            // 执行发送
            ctx.fireWrite(new RpcRequestMessage(requestGuid, writeTask.isSync(), body));
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
    public void read(SessionHandlerContext ctx, Object msg) throws IOException {
        if (msg instanceof RpcRequestMessage) {
            // 读取到一个Rpc请求消息，提交给应用层
            RpcRequestMessage requestMessage = (RpcRequestMessage) msg;
            RpcResponseChannel<?> rpcResponseChannel = new DefaultRpcResponseChannel<>(ctx.session(),
                    requestMessage.getRequestGuid(), requestMessage.isSync());

            // 检查提前反序列化字段
            final Object body = checkPreDeserialize(requestMessage.getBody());
            ctx.appEventLoop().execute(new RpcRequestCommitTask(ctx.session(), body, rpcResponseChannel));
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

    /**
     * 检查延迟初始化参数
     *
     * @return newCall or the same call
     * @throws IOException error
     */
    private Object checkLazySerialize(Object body) throws IOException {
        if (!(body instanceof RpcCall)) {
            return body;
        }

        final RpcCall<?> rpcCall = (RpcCall<?>) body;
        final int lazyIndexes = rpcCall.getLazyIndexes();
        if (lazyIndexes <= 0) {
            return rpcCall;
        }

        // bugs: 如果不创建新的list，则可能出现并发set的情况，可能导致部分线程看见错误的数据
        // 解决方案有：①防御性拷贝 ②对RpcCall对象加锁
        // 选择防御性拷贝的理由：①使用延迟序列化和提前反序列化的比例并不高 ②方法方法参数个数偏小，创建一个小list的成本较低。
        final List<Object> methodParams = rpcCall.getMethodParams();
        final ArrayList<Object> newMethodParams = new ArrayList<>(methodParams.size());

        for (int index = 0, end = methodParams.size(); index < end; index++) {
            final Object parameter = methodParams.get(index);
            final Object newParameter;

            if ((lazyIndexes & (1L << index)) != 0 && !(parameter instanceof byte[])) {
                newParameter = codec.serializeToBytes(parameter);
            } else {
                newParameter = parameter;
            }

            newMethodParams.add(newParameter);
        }

        return new RpcCall<>(rpcCall.getMethodKey(), newMethodParams, 0, rpcCall.getPreIndexes());
    }

    /**
     * 检查提前反序列化参数
     *
     * @return newCall or the same call
     * @throws IOException error
     */
    private Object checkPreDeserialize(Object body) throws IOException {
        if (!(body instanceof RpcCall)) {
            return body;
        }

        final RpcCall<?> rpcCall = (RpcCall<?>) body;
        final int preIndexes = rpcCall.getPreIndexes();
        if (preIndexes <= 0) {
            return rpcCall;
        }

        // 线程安全问题同上面
        final List<Object> methodParams = rpcCall.getMethodParams();
        final ArrayList<Object> newMethodParams = new ArrayList<>(methodParams.size());

        for (int index = 0, end = methodParams.size(); index < end; index++) {
            final Object parameter = methodParams.get(index);
            final Object newParameter;
            if ((preIndexes & (1L << index)) != 0 && parameter instanceof byte[]) {
                newParameter = codec.deserializeFromBytes((byte[]) parameter);
            } else {
                newParameter = parameter;
            }
            newMethodParams.add(newParameter);
        }

        return new RpcCall<>(rpcCall.getMethodKey(), newMethodParams, rpcCall.getLazyIndexes(), 0);
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
