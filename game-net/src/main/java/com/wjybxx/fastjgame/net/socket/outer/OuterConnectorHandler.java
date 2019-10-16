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

package com.wjybxx.fastjgame.net.socket.outer;

import com.wjybxx.fastjgame.misc.IntSequencer;
import com.wjybxx.fastjgame.net.common.ConnectAwareTask;
import com.wjybxx.fastjgame.net.common.DisconnectAwareTask;
import com.wjybxx.fastjgame.net.session.SessionDuplexHandlerAdapter;
import com.wjybxx.fastjgame.net.session.SessionHandlerContext;
import com.wjybxx.fastjgame.net.socket.MessageQueue;
import com.wjybxx.fastjgame.utils.ConcurrentUtils;

import javax.annotation.Nullable;

/**
 * session客户端方维持socket使用的handler
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/10/16
 * github - https://github.com/hl845740757
 */
public class OuterConnectorHandler extends SessionDuplexHandlerAdapter {
    /**
     * 发起验证请求的次数
     */
    private final IntSequencer verifyingSequencer = new IntSequencer(0);
    /**
     * 验证成功的次数
     */
    private final IntSequencer verifiedSequencer = new IntSequencer(0);
    /**
     * 消息队列
     */
    private final MessageQueue messageQueue = new MessageQueue();
    private HandlerState state;

    @Override
    public void handlerAdded(SessionHandlerContext ctx) throws Exception {
        super.handlerAdded(ctx);
    }

    @Override
    public void handlerRemoved(SessionHandlerContext ctx) throws Exception {
        super.handlerRemoved(ctx);
    }

    @Override
    public void tick(SessionHandlerContext ctx) throws Exception {
        super.tick(ctx);
    }

    @Override
    public void onSessionActive(SessionHandlerContext ctx) throws Exception {
        ConcurrentUtils.safeExecute(ctx.localEventLoop(), new ConnectAwareTask(ctx.session()));
        ctx.fireSessionActive();
    }

    @Override
    public void onSessionInactive(SessionHandlerContext ctx) throws Exception {
        ConcurrentUtils.safeExecute(ctx.localEventLoop(), new DisconnectAwareTask(ctx.session()));
        ctx.fireSessionInactive();
    }

    @Override
    public void read(SessionHandlerContext ctx, Object msg) {
        if (ctx.session().isActive()) {
            ctx.fireRead(msg);
        }
    }

    @Override
    public void write(SessionHandlerContext ctx, Object msg) throws Exception {
        if (ctx.session().isActive()) {

        }
    }

    @Override
    public void flush(SessionHandlerContext ctx) throws Exception {
        super.flush(ctx);
    }

    @Override
    public void close(SessionHandlerContext ctx) throws Exception {
        super.close(ctx);
    }

    // ------------------------------------------ 状态机管理 --------------------------------------------

    private void changeState(@Nullable HandlerState newState) {
        HandlerState oldState = this.state;
        if (null != oldState) {
            oldState.exit();
        }
        this.state = newState;
        if (null != newState) {
            newState.enter();
        }
    }

    private abstract class HandlerState {

        abstract void enter();

        abstract void tick();

        abstract void exit();
    }
}
