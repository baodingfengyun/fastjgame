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

package com.wjybxx.fastjgame.net.session;

import com.wjybxx.fastjgame.net.eventloop.NetEventLoop;
import com.wjybxx.fastjgame.net.manager.NetManagerWrapper;
import com.wjybxx.fastjgame.net.misc.NetContext;
import com.wjybxx.fastjgame.util.annotation.Internal;
import com.wjybxx.fastjgame.util.concurrent.EventLoop;
import com.wjybxx.fastjgame.util.concurrent.EventLoopUtils;
import com.wjybxx.fastjgame.util.timer.TimerHandle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Session的模板实现。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/4
 * github - https://github.com/hl845740757
 */
public abstract class AbstractSession implements Session {

    private static final Logger logger = LoggerFactory.getLogger(AbstractSession.class);

    private static final int ST_BOUND = 0;
    private static final int ST_CONNECTED = 1;
    private static final int ST_CLOSED = 2;

    private static final int TICK_INTERVAL = 20;

    private final NetContext netContext;
    private final String sessionId;
    private final SessionConfig config;
    /**
     * session所属的注册表
     */
    private final SessionRegistry sessionRegistry;
    /**
     * session绑定到的EventLoop
     */
    private final NetEventLoop netEventLoop;
    /**
     * session关联的管道
     */
    private final SessionPipeline pipeline;
    /**
     * session状态
     */
    private final AtomicInteger stateHolder = new AtomicInteger(ST_BOUND);
    /**
     * tick用的handle
     */
    private final TimerHandle tickHandle;
    /**
     * 附加属性 - 非volatile，只有用户线程可以使用
     */
    private Object attachment;

    protected AbstractSession(NetContext netContext, String sessionId, SessionConfig config,
                              NetManagerWrapper managerWrapper, SessionRegistry sessionRegistry) {
        this.netContext = netContext;
        this.sessionId = sessionId;
        this.config = config;
        this.sessionRegistry = sessionRegistry;
        this.pipeline = new DefaultSessionPipeline(this, managerWrapper.getNetTimeManager());
        this.netEventLoop = managerWrapper.getNetEventLoopManager().getEventLoop();
        this.tickHandle = managerWrapper.getNetTimerManager().newHeartbeatTimer(TICK_INTERVAL, this::tick);
        sessionRegistry.registerSession(this);
    }

    @Override
    public final String sessionId() {
        return sessionId;
    }

    @Override
    public SessionConfig config() {
        return config;
    }

    @Override
    public final EventLoop appEventLoop() {
        return netContext.appEventLoop();
    }

    @Override
    public final NetEventLoop netEventLoop() {
        return netEventLoop;
    }

    @Override
    public final <T> T attach(@Nullable Object newData) {
        if (appEventLoop().inEventLoop()) {
            @SuppressWarnings("unchecked")
            T pre = (T) attachment;
            this.attachment = newData;
            return pre;
        } else {
            throw new IllegalStateException("Unsafe op");
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public final <T> T attachment() {
        if (appEventLoop().inEventLoop()) {
            return (T) attachment;
        } else {
            throw new IllegalStateException("Unsafe op");
        }
    }

    @Override
    public final boolean isActive() {
        return stateHolder.get() == ST_CONNECTED;
    }

    @Override
    public boolean isClosed() {
        return stateHolder.get() == ST_CLOSED;
    }

    @Override
    public final void close() {
        final int oldState = stateHolder.getAndSet(ST_CLOSED);
        if (oldState == ST_CLOSED) {
            // 非首次调用
            return;
        }

        // 可能是网络层关闭
        EventLoopUtils.executeOrRun(netEventLoop, () -> closeAndCheckNotify(oldState));
    }

    private void closeAndCheckNotify(int oldState) {
        doCloseSafely();
        checkNotify(oldState);
    }

    private void doCloseSafely() {
        try {
            sessionRegistry.removeSession(sessionId);
            tickHandle.close();
            pipeline.fireClose();
        } catch (Throwable t) {
            logger.warn("doClose caught exception", t);
        }
    }

    private void checkNotify(int oldState) {
        if (oldState == ST_CONNECTED) {
            // 之前已建立连接，则需要调用inactive方法
            pipeline.fireSessionInactive();
        }
    }

    @Override
    public final SessionPipeline pipeline() {
        ensureInNetEventLoop();
        return pipeline;
    }

    @Internal
    @Override
    public void fireRead(@Nonnull Object msg) {
        ensureInNetEventLoop();
        pipeline.fireRead(msg);
    }

    @Internal
    @Override
    public void fireWrite(@Nonnull Object msg) {
        ensureInNetEventLoop();
        pipeline.fireWrite(msg);
    }

    @Override
    public void fireWriteAndFlush(@Nonnull Object msg) {
        ensureInNetEventLoop();
        pipeline.fireWriteAndFlush(msg);
    }

    /**
     * @return 如果切换为激活状态，则返回true
     */
    @Internal
    public boolean tryActive() {
        ensureInNetEventLoop();
        return stateHolder.compareAndSet(ST_BOUND, ST_CONNECTED);
    }

    /**
     * tick刷帧 - 不暴露给应用层
     */
    private void tick(TimerHandle handle) {
        pipeline.fireTick();
    }

    /**
     * 网络层强制关闭，不调用事件通知
     */
    @Internal
    public final void closeForcibly() {
        ensureInNetEventLoop();

        final int oldState = stateHolder.getAndSet(ST_CLOSED);
        if (oldState == ST_CLOSED) {
            // 已关闭
            return;
        }
        doCloseSafely();
    }

    /**
     * 线程保护
     */
    private void ensureInNetEventLoop() {
        EventLoopUtils.ensureInEventLoop(netEventLoop);
    }

    @Override
    public final int hashCode() {
        return sessionId.hashCode();
    }

    @Override
    public final boolean equals(Object obj) {
        return this == obj;
    }

    @Override
    public final int compareTo(@Nonnull Session o) {
        return sessionId.compareTo(o.sessionId());
    }
}
