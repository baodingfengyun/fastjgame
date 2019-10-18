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
import com.wjybxx.fastjgame.net.session.SessionHandlerContext;
import com.wjybxx.fastjgame.net.session.SessionInboundHandlerAdapter;

/**
 * 心跳支持
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/10/1
 * github - https://github.com/hl845740757
 */
public class PongSupportHandler extends SessionInboundHandlerAdapter {

    private NetTimeManager timeManager;
    private long sessionTimeoutMs;
    private long lastReadTime;

    @Override
    public void handlerAdded(SessionHandlerContext ctx) throws Exception {
        // 缓存 - 减少栈深度
        timeManager = ctx.managerWrapper().getNetTimeManager();
        sessionTimeoutMs = ctx.session().config().getSessionTimeoutMs();

        lastReadTime = timeManager.getSystemMillTime();
    }

    @Override
    public void tick(SessionHandlerContext ctx) {
        if (timeManager.getSystemMillTime() - lastReadTime > sessionTimeoutMs) {
            // session超时
            ctx.session().close();
        }
    }

    @Override
    public void read(SessionHandlerContext ctx, Object msg) {
        lastReadTime = timeManager.getSystemMillTime();
        if (msg == PingPongMessage.INSTANCE) {
            // 读取到一个需要返回的心跳包，立即返回一个心跳包
            // 从当前位置直接返回心跳响应
            ctx.fireWriteAndFlush(PingPongMessage.INSTANCE);
        } else {
            if (msg != PingPongMessage.INSTANCE2) {
                // 读取到一个逻辑消息
                ctx.fireRead(msg);
            }
        }
    }
}
