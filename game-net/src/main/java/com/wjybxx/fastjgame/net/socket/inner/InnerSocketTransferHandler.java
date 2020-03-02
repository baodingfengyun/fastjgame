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

import com.wjybxx.fastjgame.net.rpc.ConnectAwareTask;
import com.wjybxx.fastjgame.net.rpc.DisconnectAwareTask;
import com.wjybxx.fastjgame.net.rpc.NetMessage;
import com.wjybxx.fastjgame.net.rpc.PingPongMessage;
import com.wjybxx.fastjgame.net.session.SessionDuplexHandlerAdapter;
import com.wjybxx.fastjgame.net.session.SessionHandlerContext;
import com.wjybxx.fastjgame.net.socket.*;
import com.wjybxx.fastjgame.net.utils.NetUtils;
import com.wjybxx.fastjgame.utils.ConcurrentUtils;
import io.netty.channel.Channel;

import java.util.LinkedList;

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
    private int maxPendingMessages;
    /**
     * 缓冲区 - 减少与netty的交互
     */
    private LinkedList<SocketMessage> buffer = new LinkedList<>();

    InnerSocketTransferHandler(Channel channel) {
        this.channel = channel;
    }

    @Override
    public void handlerAdded(SessionHandlerContext ctx) throws Exception {
        SocketSessionConfig config = (SocketSessionConfig) ctx.session().config();
        maxPendingMessages = config.maxPendingMessages();
    }

    @Override
    public void tick(SessionHandlerContext ctx) {
        if (buffer.size() > 0) {
            doFlush();
        }
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
    public void read(SessionHandlerContext ctx, Object msg) {
        if (ctx.session().isClosed()) {
            // session已关闭，丢弃消息
            NetUtils.closeQuietly(((SocketEvent) msg).channel());
            return;
        }

        if (msg instanceof SocketMessageEvent) {
            // 消息事件 - 它出现的概率更高，因此放在前面
            ctx.fireRead(((SocketMessageEvent) msg).getWrappedMessage());
            return;
        }

        if (msg instanceof SocketPingPongEvent) {
            // 心跳事件
            ctx.fireRead(((SocketPingPongEvent) msg).getPingOrPong());
            return;
        }

        if (msg instanceof SocketChannelInactiveEvent) {
            // socket断开事件 - 内网不断线重连，不使用消息确认机制，socket断开就关闭session
            ctx.session().close();
            return;
        }

        // 期望之外的消息
        NetUtils.closeQuietly(((SocketEvent) msg).channel());
    }

    @Override
    public void write(SessionHandlerContext ctx, Object msg) throws Exception {
        if (ctx.session().isClosed()) {
            // session已关闭，丢弃消息
            return;
        }
        if (msg == PingPongMessage.PING || msg == PingPongMessage.PONG) {
            // 心跳包
            channel.writeAndFlush(new InnerPingPongMessageTO((PingPongMessage) msg));
        } else {
            // 用户数据包
            buffer.add(new InnerSocketMessage((NetMessage) msg));

            if (buffer.size() >= maxPendingMessages) {
                // 检测是否需要清空缓冲区了
                doFlush();
            }
        }
        // else
    }

    @Override
    public void flush(SessionHandlerContext ctx) throws Exception {
        if (ctx.session().isClosed()) {
            // session已关闭，丢弃消息
            return;
        }

        if (buffer.size() > 0) {
            doFlush();
        }
    }

    private void doFlush() {
        if (buffer.size() == 1) {
            channel.writeAndFlush(buffer.pollFirst());
        } else {
            channel.writeAndFlush(new InnerBatchSocketMessageTO(buffer));
            buffer = new LinkedList<>();
        }
    }

    @Override
    public void close(SessionHandlerContext ctx) throws Exception {
        NetUtils.closeQuietly(channel);
    }
}
