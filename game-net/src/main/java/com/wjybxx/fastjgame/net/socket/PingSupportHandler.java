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

/**
 * 心跳支持
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/10/1
 * github - https://github.com/hl845740757
 */
public class PingSupportHandler extends SessionDuplexHandlerAdapter {

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
    public void tick(SessionHandlerContext ctx) throws Exception {
        if (timeManager.getSystemMillTime() - lastReadTime > sessionTimeoutMs) {
            // session超时
            ctx.session().close();
            return;
        }
        if (timeManager.getSystemMillTime() - lastWriteTime > pingIntervalMs) {
            // 有一段时间没发送消息了，发一个包
            // 从当前位置开始发送心跳包 - 否则如果被拦截，时间得不到更新就爆炸
            write(ctx, PingPongMessage.INSTANCE);
        }
    }

    @Override
    public void read(SessionHandlerContext ctx, Object msg) {
        lastReadTime = timeManager.getSystemMillTime();
        if (msg != PingPongMessage.INSTANCE && msg != PingPongMessage.INSTANCE2) {
            // 非心跳包
            ctx.fireRead(msg);
        }
    }

    @Override
    public void write(SessionHandlerContext ctx, Object msg) throws Exception {
        lastWriteTime = timeManager.getSystemMillTime();
        ctx.fireWrite(msg);
    }
}
