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

package com.wjybxx.fastjgame.net.socket;

import com.wjybxx.fastjgame.manager.NetTimeManager;
import com.wjybxx.fastjgame.net.common.PingPongMessage;
import com.wjybxx.fastjgame.net.session.SessionDuplexHandlerAdapter;
import com.wjybxx.fastjgame.net.session.SessionHandlerContext;
import com.wjybxx.fastjgame.timer.FixedDelayHandle;

/**
 * 双向心跳支持。
 * <p>
 * ping包的特性：
 * 1. 对方收到以后会立即返回一个pong包 - 具有立即返回机制，而业务逻辑消息并不具备该性质。
 * 2. ping、pong包不会进入缓存队列，会直接进行发送。
 * <p>
 * Q: 为何使用ping-ping心跳机制，而不是ping-pong机制？
 * A:
 * 1. 在具有流量控制机制的情况下，使用ping-ping心跳对服务器更友好，服务器也可以及时的更新自己的填充队列。
 * 而使用ping-pong机制的话，服务器是被动的方式更新填充队列，可能长时间无法更新填充队列，导致长时间无法发包。
 * 2. 使用ping-ping可以更快的识别网络故障
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/10/1
 * github - https://github.com/hl845740757
 */
public class PingPingSupportHandler extends SessionDuplexHandlerAdapter {

    private NetTimeManager timeManager;
    private long pingIntervalMs;
    private long sessionTimeoutMs;

    private long lastReadTime;
    private long lastWriteTime;

    private SessionHandlerContext ctx;
    private FixedDelayHandle pingTimerHandle;

    @Override
    public void handlerAdded(SessionHandlerContext ctx) throws Exception {
        this.ctx = ctx;
        // 缓存减少堆栈深度
        timeManager = ctx.managerWrapper().getNetTimeManager();

        SocketSessionConfig config = (SocketSessionConfig) ctx.session().config();
        pingIntervalMs = config.pingIntervalMs();
        sessionTimeoutMs = config.getSessionTimeoutMs();

        lastReadTime = timeManager.getSystemMillTime();
        lastWriteTime = timeManager.getSystemMillTime();

        pingTimerHandle = ctx.managerWrapper().getNetTimerManager().newFixedDelay(pingIntervalMs, pingIntervalMs, this::checkPing);
    }

    private void checkPing(FixedDelayHandle handle) throws Exception {
        if (timeManager.getSystemMillTime() - lastReadTime > pingIntervalMs
                || timeManager.getSystemMillTime() - lastWriteTime > pingIntervalMs) {
            // 尝试发一个心跳包 - 从当前位置开始发送心跳包
            write(ctx, PingPongMessage.PING);
        }
    }

    @Override
    public void tick(SessionHandlerContext ctx) throws Exception {
        if (timeManager.getSystemMillTime() - lastReadTime > sessionTimeoutMs) {
            // session超时
            ctx.session().close();
        }
    }

    @Override
    public void read(SessionHandlerContext ctx, Object msg) throws Exception {
        lastReadTime = timeManager.getSystemMillTime();
        if (msg == PingPongMessage.PING) {
            // 心跳请求包，需要立即返回
            write(ctx, PingPongMessage.PONG);
            return;
        }
        if (msg != PingPongMessage.PONG) {
            // 非心跳包
            ctx.fireRead(msg);
        }
    }

    @Override
    public void write(SessionHandlerContext ctx, Object msg) throws Exception {
        lastWriteTime = timeManager.getSystemMillTime();
        ctx.fireWrite(msg);
    }

    @Override
    public void close(SessionHandlerContext ctx) throws Exception {
        try {
            pingTimerHandle.cancel();
        } finally {
            ctx.fireClose();
        }
    }
}
