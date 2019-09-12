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

package com.wjybxx.fastjgame.net;

import com.wjybxx.fastjgame.concurrent.EventLoop;
import com.wjybxx.fastjgame.eventloop.NetEventLoop;
import com.wjybxx.fastjgame.utils.EventLoopUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

/**
 * {@link Sender}的模板实现。
 * 同步消息的发送都由这里来实现，子类只负责异步消息的发送。
 * 超类会统一将消息封装为任务，由子类觉得如何提交到网络层，但是子类必须保证提交的时序。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/30
 * github - https://github.com/hl845740757
 */
@ThreadSafe
public abstract class AbstractSender implements Sender {

    private static final Logger logger = LoggerFactory.getLogger(AbstractSender.class);

    protected final AbstractSession session;

    protected AbstractSender(AbstractSession session) {
        this.session = session;
    }

    @Override
    public final Session session() {
        return session;
    }

    @Override
    public final void send(@Nonnull Object message, boolean immediate) {
        // 逻辑层检测，会话已关闭，立即返回
        if (!isActive()) {
            logger.debug("session is already closed, send message failed.");
            return;
        }
        if (immediate) {
            netEventLoop().execute(new OneWayMessageTask(session, message, immediate));
        } else {
            addSenderTask(new OneWayMessageTask(session, message, immediate));
        }
    }

    @Override
    public final void call(@Nonnull Object request, @Nonnull RpcCallback callback, long timeoutMs, boolean immediate) {
        // 参数校验，必须有过期时间
        if (timeoutMs <= 0) {
            throw new IllegalArgumentException("timeoutMs");
        }
        // 逻辑层校验，会话已关闭
        if (!isActive()) {
            // 始终保持回调执行在用户线程
            EventLoopUtils.executeOrRun(userEventLoop(), () -> {
                callback.onComplete(RpcResponse.SESSION_CLOSED);
            });
            return;
        }
        if (immediate) {
            netEventLoop().execute(new RpcRequestTask(session, request, timeoutMs, callback, immediate));
        } else {
            addSenderTask(new RpcRequestTask(session, request, timeoutMs, callback, immediate));
        }
    }

    /**
     * 子类通过实现该方法实现自己的发送策略。
     *
     * @param task 一个数据发送请求
     * @apiNote 必须保证消息的时序
     */
    protected abstract void addSenderTask(SenderTask task);

    @Nonnull
    @Override
    public final RpcResponse sync(@Nonnull Object request, long timeoutMs, boolean immediate) {
        if (timeoutMs <= 0) {
            throw new IllegalArgumentException("timeoutMs");
        }
        // 逻辑层校验，会话已关闭，立即返回结果
        if (!isActive()) {
            return RpcResponse.SESSION_CLOSED;
        }
        final RpcPromise rpcPromise = netEventLoop().newRpcPromise(userEventLoop(), timeoutMs);
        final RpcRequestTask2 requestTask2 = new RpcRequestTask2(session, request, timeoutMs, rpcPromise, immediate);
        if (immediate) {
            // 直接提交到网络层执行
            netEventLoop().execute(requestTask2);
        } else {
            // 添加到任务列表
            addSenderTask(requestTask2);
            // 确保能安全的阻塞等待结果
            flush();
        }
        // RpcPromise保证了不会等待超过限时时间
        rpcPromise.awaitUninterruptibly();
        // 一定有结果
        return rpcPromise.getNow();
    }

    @Override
    public final <T> RpcResponseChannel<T> newResponseChannel(long requestGuid, boolean immediate) {
        return new DefaultRpcResponseChannel<>(this, requestGuid, immediate);
    }

    protected boolean isActive() {
        return session.isActive();
    }

    protected NetEventLoop netEventLoop() {
        return session.netEventLoop();
    }

    protected EventLoop userEventLoop() {
        return session.localEventLoop();
    }

    /**
     * 主要用于减少lambda表达式，也方便未来扩展
     */
    protected interface SenderTask extends Runnable {

        /**
         * 执行发送操作，运行在网络线程下。
         * 实现{@link Runnable}接口可以减少lambda表达式。
         */
        void run();

        /**
         * 执行取消操作，运行在用户线程下。
         */
        void cancel();
    }

    protected static class OneWayMessageTask implements SenderTask {

        private final AbstractSession session;
        private final Object message;
        private final boolean immediate;

        private OneWayMessageTask(AbstractSession session, Object message, boolean immediate) {
            this.session = session;
            this.message = message;
            this.immediate = immediate;
        }

        @Override
        public void run() {
            session.sendOneWayMessage(message, immediate);
        }

        @Override
        public void cancel() {
            // do nothing
        }
    }

    protected static class RpcRequestTask implements SenderTask {

        private final AbstractSession session;
        private final Object request;
        private final long timeoutMs;
        private final RpcCallback rpcCallback;
        private final boolean immediate;

        private RpcRequestTask(AbstractSession session, Object request, long timeoutMs, RpcCallback rpcCallback, boolean immediate) {
            this.session = session;
            this.request = request;
            this.timeoutMs = timeoutMs;
            this.rpcCallback = rpcCallback;
            this.immediate = immediate;
        }

        @Override
        public void run() {
            session.sendRpcRequest(request, timeoutMs, rpcCallback, immediate);
        }

        @Override
        public void cancel() {
            // 要保证回调执行
            rpcCallback.onComplete(RpcResponse.SESSION_CLOSED);
        }
    }

    private static class RpcRequestTask2 implements SenderTask {

        private final AbstractSession session;
        private final Object request;
        private final long timeoutMs;
        private final RpcPromise rpcPromise;
        private final boolean immediate;

        private RpcRequestTask2(AbstractSession session, Object request, long timeoutMs, RpcPromise rpcPromise, boolean immediate) {
            this.session = session;
            this.request = request;
            this.timeoutMs = timeoutMs;
            this.rpcPromise = rpcPromise;
            this.immediate = immediate;
        }

        @Override
        public void run() {
            session.sendRpcRequest(request, timeoutMs, rpcPromise, immediate);
        }

        @Override
        public void cancel() {
            rpcPromise.trySuccess(RpcResponse.SESSION_CLOSED);
        }
    }

    protected static class RpcResponseTask implements SenderTask {

        private final AbstractSession session;
        private final long requestGuid;
        private final RpcResponse rpcResponse;
        private final boolean immediate;

        private RpcResponseTask(AbstractSession session, long requestGuid, RpcResponse rpcResponse, boolean immediate) {
            this.session = session;
            this.requestGuid = requestGuid;
            this.rpcResponse = rpcResponse;
            this.immediate = immediate;
        }

        @Override
        public void run() {
            session.sendRpcResponse(requestGuid, immediate, rpcResponse);
        }

        @Override
        public void cancel() {
            // do nothing
        }
    }

    private static class DefaultRpcResponseChannel<T> extends AbstractRpcResponseChannel<T> {

        private final AbstractSender sender;
        private final long requestGuid;
        private final boolean immediate;

        private DefaultRpcResponseChannel(AbstractSender sender, long requestGuid, boolean immediate) {
            this.sender = sender;
            this.requestGuid = requestGuid;
            this.immediate = immediate;
        }

        @Override
        protected void doWrite(RpcResponse rpcResponse) {
            if (sender.isActive()) {
                if (immediate) {
                    sender.netEventLoop().execute(new RpcResponseTask(sender.session, requestGuid, rpcResponse, immediate));
                } else {
                    sender.addSenderTask(new RpcResponseTask(sender.session, requestGuid, rpcResponse, immediate));
                }

            }
        }
    }
}
