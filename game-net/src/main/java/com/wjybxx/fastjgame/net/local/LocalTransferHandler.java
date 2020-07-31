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

package com.wjybxx.fastjgame.net.local;

import com.wjybxx.fastjgame.net.session.*;
import com.wjybxx.fastjgame.utils.concurrent.ConcurrentUtils;

/**
 * JVM 内部传输实现 - 它是出站的最后一个处理器，因此也是真正实现关闭的handler
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/9/26
 * github - https://github.com/hl845740757
 */
public class LocalTransferHandler extends SessionDuplexHandlerAdapter {

    private Session remoteSession;

    public void setRemoteSession(Session remoteSession) {
        this.remoteSession = remoteSession;
    }

    @Override
    public void onSessionActive(SessionHandlerContext ctx) throws Exception {
        ConcurrentUtils.safeExecute(ctx.appEventLoop(), new ConnectAwareTask(ctx.session()));
        ctx.fireSessionActive();
    }

    @Override
    public void onSessionInactive(SessionHandlerContext ctx) throws Exception {
        ConcurrentUtils.safeExecute(ctx.appEventLoop(), new DisconnectAwareTask(ctx.session()));
        ctx.fireSessionInactive();
    }

    @Override
    public void write(SessionHandlerContext ctx, Object msg) throws Exception {
        if (!ctx.session().isClosed()) {
            // 直接触发另一个session的读事件
            remoteSession.fireRead(msg);
        }
    }

    @Override
    public void flush(SessionHandlerContext ctx) throws Exception {
        // 终止
    }

    @Override
    public void read(SessionHandlerContext ctx, Object msg) {
        if (!ctx.session().isClosed()) {
            ctx.fireRead(msg);
        }
    }

    @Override
    public void close(SessionHandlerContext ctx) throws Exception {
        // 下一帧关闭对方（总是使对方晚于自己关闭）
        if (!remoteSession.isClosed()) {
            // 存为临时变量，少捕获变量(不捕获this)
            final Session remoteSession = this.remoteSession;
            ctx.timerSystem().nextTick(handle -> remoteSession.close());
        }
    }
}
