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

import com.wjybxx.fastjgame.net.common.ConnectAwareTask;
import com.wjybxx.fastjgame.net.common.DisconnectAwareTask;
import com.wjybxx.fastjgame.net.common.NetMessage;
import com.wjybxx.fastjgame.net.session.SessionDuplexHandlerAdapter;
import com.wjybxx.fastjgame.net.session.SessionHandlerContext;
import com.wjybxx.fastjgame.net.socket.SocketChannelInactiveEvent;
import com.wjybxx.fastjgame.net.socket.SocketEvent;
import com.wjybxx.fastjgame.net.socket.SocketMessageEvent;
import com.wjybxx.fastjgame.utils.ConcurrentUtils;
import com.wjybxx.fastjgame.utils.NetUtils;
import io.netty.channel.Channel;

/**
 * 内网服务器之间传输支持。
 * 1. 由于它真正的向{@link Channel}中写入数据，因此也负责关闭channel
 * 2. 它负责过滤无效的消息
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/10/1
 * github - https://github.com/hl845740757
 */
public class InnerSocketTransferHandler extends SessionDuplexHandlerAdapter {

    /**
     * 真正通信的channel。
     */
    private final Channel channel;
    /**
     * flush之前的消息数
     */
    private int msgCount;

    InnerSocketTransferHandler(Channel channel) {
        this.channel = channel;
    }

    @Override
    public void tick(SessionHandlerContext ctx) {
        if (msgCount > 0) {
            doFlush();
        }
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
        SocketEvent socketEvent = (SocketEvent) msg;
        if (ctx.session().isClosed()) {
            // session已关闭，丢弃消息
            NetUtils.closeQuietly(socketEvent.channel());
            return;
        }
        if (socketEvent instanceof SocketMessageEvent) {
            // 接收到一个消息 - 它出现的概率更高，因此放在断开连接事件前
            ctx.fireRead(((SocketMessageEvent) msg).getWrappedMessage());
            return;
        }
        if (socketEvent instanceof SocketChannelInactiveEvent) {
            // socket断开事件 - 内网不断线重连，不使用消息确认机制，socket断开就关闭session
            ctx.session().close();
        }
    }

    @Override
    public void write(SessionHandlerContext ctx, Object msg) throws Exception {
        if (ctx.session().isClosed()) {
            // session已关闭，丢弃消息
            return;
        }
        msgCount++;
        channel.write(new InnerSocketMessage((NetMessage) msg), channel.voidPromise());
        // else
    }

    @Override
    public void flush(SessionHandlerContext ctx) throws Exception {
        if (ctx.session().isClosed()) {
            // session已关闭，丢弃消息
            return;
        }

        if (msgCount > 0) {
            doFlush();
        }
    }

    private void doFlush() {
        msgCount = 0;
        channel.flush();
    }

    @Override
    public void close(SessionHandlerContext ctx) throws Exception {
        NetUtils.closeQuietly(channel);
    }
}
