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

package com.wjybxx.fastjgame.net.pipeline;

import com.wjybxx.fastjgame.concurrent.EventLoop;
import com.wjybxx.fastjgame.concurrent.Promise;
import com.wjybxx.fastjgame.eventloop.NetEventLoop;
import com.wjybxx.fastjgame.manager.NetManagerWrapper;
import com.wjybxx.fastjgame.net.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * {@link SessionHandlerContext}的模板实现。
 * 它并不像netty的{@link io.netty.channel.ChannelHandlerContext}那么复杂。
 * Q: 为什么可以简化？
 * A: 因为保证了{@link SessionPipeline}的所有{@link SessionHandler}都在同一个线程中执行，且取消了动态的组合、
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/9/25
 * github - https://github.com/hl845740757
 */
abstract class AbstractSessionHandlerContext implements SessionHandlerContext {

    private static final Logger logger = LoggerFactory.getLogger(AbstractSessionHandlerContext.class);

    private final DefaultSessionPipeline pipeline;
    private final NetManagerWrapper managerWrapper;
    /**
     * 上一个handlerContext
     */
    AbstractSessionHandlerContext prev;
    /**
     * 下一个handlerContext
     */
    AbstractSessionHandlerContext next;

    AbstractSessionHandlerContext(DefaultSessionPipeline pipeline, NetManagerWrapper managerWrapper) {
        this.pipeline = pipeline;
        this.managerWrapper = managerWrapper;
    }

    @Override
    public SessionPipeline pipeline() {
        return pipeline;
    }

    @Override
    public Session session() {
        return pipeline.session();
    }

    @Override
    public NetEventLoop netEventLoop() {
        return pipeline.netEventLoop();
    }

    @Override
    public EventLoop localEventLoop() {
        return pipeline.localEventLoop();
    }

    @Override
    public NetManagerWrapper managerWrapper() {
        return managerWrapper;
    }

    @Override
    public void tick() {
        handler().tick(this);
    }

    // --------------------------------------------------- inbound ----------------------------------------------

    @Override
    public void fireSessionActive() {
        final AbstractSessionHandlerContext nextInboundContext = findNextInboundContext();
        invokeSessionActive(nextInboundContext);
    }

    private static void invokeSessionActive(AbstractSessionHandlerContext nextInboundContext) {
        final SessionInboundHandler handler = (SessionInboundHandler) nextInboundContext.handler();
        try {
            handler.onSessionActive(nextInboundContext);
        } catch (Throwable e) {
            notifyExceptionCaught(e, nextInboundContext);
        }
    }

    @Override
    public void fireSessionInactive() {
        final AbstractSessionHandlerContext nextInboundContext = findNextInboundContext();
        invokeSessionInactive(nextInboundContext);
    }

    private static void invokeSessionInactive(AbstractSessionHandlerContext nextInboundContext) {
        final SessionInboundHandler handler = (SessionInboundHandler) nextInboundContext.handler();
        try {
            handler.onSessionInactive(nextInboundContext);
        } catch (Throwable e) {
            notifyExceptionCaught(e, nextInboundContext);
        }
    }

    @Override
    public void fireRead(@Nullable Object msg) {
        final AbstractSessionHandlerContext nextInboundContext = findNextInboundContext();
        invokeRead(msg, nextInboundContext);
    }

    private static void invokeRead(@Nullable Object message, AbstractSessionHandlerContext nextInboundContext) {
        final SessionInboundHandler handler = (SessionInboundHandler) nextInboundContext.handler();
        try {
            handler.read(nextInboundContext, message);
        } catch (Throwable e) {
            notifyExceptionCaught(e, nextInboundContext);
        }
    }

    @Override
    public void fireExceptionCaught(Throwable cause) {
        final AbstractSessionHandlerContext nextInboundContext = findNextInboundContext();
        final SessionInboundHandler handler = (SessionInboundHandler) nextInboundContext.handler();
        try {
            handler.onExceptionCaught(nextInboundContext, cause);
        } catch (Throwable e) {
            logger.warn("An exception was thrown by a user handler while handling an exceptionCaught event", e);
        }
    }

    private static void notifyExceptionCaught(Throwable cause, AbstractSessionHandlerContext curContext) {
        final AbstractSessionHandlerContext nextInboundContext = curContext.findNextInboundContext();
        final SessionInboundHandler handler = (SessionInboundHandler) nextInboundContext.handler();
        try {
            handler.onExceptionCaught(nextInboundContext, cause);
        } catch (Throwable e) {
            logger.warn("An exception was thrown by a user handler while handling an exceptionCaught event", e);
        }
    }
    // --------------------------------------------------- outbound ----------------------------------------------

    @Override
    public void write(@Nonnull Object msg) {
        final AbstractSessionHandlerContext nextOutboundContext = findNextOutboundContext();
        invokeWrite(msg, nextOutboundContext);
    }

    private static void invokeWrite(@Nonnull Object msg, AbstractSessionHandlerContext nextOutboundContext) {
        final SessionOutboundHandler handler = (SessionOutboundHandler) nextOutboundContext.handler();
        try {
            handler.write(nextOutboundContext, msg);
        } catch (Throwable e) {
            notifyExceptionCaught(e, nextOutboundContext);
        }
    }

    @Override
    public void flush() {
        final AbstractSessionHandlerContext nextOutboundContext = findNextOutboundContext();
        invokeFlush(nextOutboundContext);
    }

    private static void invokeFlush(AbstractSessionHandlerContext nextOutboundContext) {
        final SessionOutboundHandler handler = (SessionOutboundHandler) nextOutboundContext.handler();
        try {
            handler.flush(nextOutboundContext);
        } catch (Throwable e) {
            notifyExceptionCaught(e, nextOutboundContext);
        }
    }

    @Override
    public void writeAndFlush(@Nonnull Object msg) {
        final AbstractSessionHandlerContext nextOutboundContext = findNextOutboundContext();
        invokeWrite(msg, nextOutboundContext);
        invokeFlush(nextOutboundContext);
    }

    @Override
    public void close(Promise<?> promise) {
        final AbstractSessionHandlerContext nextOutboundContext = findNextOutboundContext();
        invokeClose(promise, nextOutboundContext);
    }

    private void invokeClose(Promise<?> promise, AbstractSessionHandlerContext nextOutboundContext) {
        final SessionOutboundHandler handler = (SessionOutboundHandler) nextOutboundContext.handler();
        try {
            handler.close(nextOutboundContext, promise);
        } catch (Throwable e) {
            notifyExceptionCaught(e, nextOutboundContext);
        }
    }

    /**
     * 寻找下一个入站处理器 (头部到尾部) - tail必须实现inboundHandler;
     * 此外：由于异常只有inboundHandler有，因此head也必须事件inboundHandler
     *
     * @return ctx
     */
    @Nonnull
    private AbstractSessionHandlerContext findNextInboundContext() {
        AbstractSessionHandlerContext ctx = this;
        do {
            ctx = ctx.next;
        } while (!ctx.isInbound());
        return ctx;
    }

    /**
     * 寻找下一个出站处理器 (尾部到头部) - head必须实现outboundHandler。
     *
     * @return ctx
     */
    @Nonnull
    private AbstractSessionHandlerContext findNextOutboundContext() {
        AbstractSessionHandlerContext ctx = this;
        do {
            ctx = ctx.prev;
        } while (!ctx.isOutbound());
        return ctx;
    }

    /**
     * 管理的handler是否是入站处理器。
     * 注意：与{@link #isOutbound()}并不一定互斥
     *
     * @return true/false
     */
    protected abstract boolean isInbound();

    /**
     * 管理的handler是否是出站处理器
     *
     * @return true/false
     */
    protected abstract boolean isOutbound();
}
