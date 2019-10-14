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

package com.wjybxx.fastjgame.net.socket.inner;

import com.wjybxx.fastjgame.manager.NetTimeManager;
import com.wjybxx.fastjgame.net.common.PingPongMessage;
import com.wjybxx.fastjgame.net.session.SessionDuplexHandlerAdapter;
import com.wjybxx.fastjgame.net.session.SessionHandlerContext;

/**
 * 心跳支持
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/10/1
 * github - https://github.com/hl845740757
 */
public class InnerPingSupportHandler extends SessionDuplexHandlerAdapter {

    private NetTimeManager timeManager;
    private long lastWriteTime;
    private long lastReadTime;
    private long pingIntervalMs;
    private long sessionTimeoutMs;

    @Override
    public void handlerAdded(SessionHandlerContext ctx) throws Exception {
        // 缓存减少堆栈深度
        timeManager = ctx.managerWrapper().getNetTimeManager();
        pingIntervalMs = ctx.session().config().getPingIntervalMs();
        sessionTimeoutMs = ctx.session().config().getSessionTimeoutMs();

        lastWriteTime = timeManager.getSystemMillTime();
        lastReadTime = timeManager.getSystemMillTime();
    }

    @Override
    public void tick(SessionHandlerContext ctx) {
        // session超时
        if (timeManager.getSystemMillTime() - lastReadTime > sessionTimeoutMs) {
            ctx.session().close();
            return;
        }
        // 有一段时间没发送消息了，发一个包
        if (timeManager.getSystemMillTime() - lastWriteTime > pingIntervalMs) {
            // 这里一定要标记发送时间，否则如果fireWrite方法被拦截就爆炸了
            lastWriteTime = timeManager.getSystemMillTime();
            ctx.session().fireWriteAndFlush(PingPongMessage.INSTANCE);
        }
    }

    @Override
    public void read(SessionHandlerContext ctx, Object msg) {
        lastReadTime = timeManager.getSystemMillTime();
        if (msg != PingPongMessage.INSTANCE) {
            ctx.fireRead(msg);
        }
    }

    @Override
    public void write(SessionHandlerContext ctx, Object msg) throws Exception {
        lastWriteTime = timeManager.getSystemMillTime();
        ctx.fireWrite(msg);
    }
}
