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

import com.wjybxx.fastjgame.concurrent.Promise;
import com.wjybxx.fastjgame.net.session.SessionDuplexHandlerAdapter;
import com.wjybxx.fastjgame.net.session.SessionHandlerContext;
import com.wjybxx.fastjgame.net.socket.SocketEvent;
import com.wjybxx.fastjgame.net.socket.SocketSessionImp;
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
     * 内网不执行重连机制 - channel不会改变，因此可以缓存
     */
    private Channel channel;

    @Override
    public void init(SessionHandlerContext ctx) throws Exception {
        this.channel = ((SocketSessionImp) ctx.session()).channel();
    }

    @Override
    public void read(SessionHandlerContext ctx, Object msg) {
        if (msg instanceof SocketEvent) {
            SocketEvent socketEvent = (SocketEvent) msg;
            if (socketEvent.channel() == channel) {
                ctx.fireRead(msg);
            }
            // else 过滤丢弃
        } else {
            ctx.fireRead(msg);
        }
    }

    @Override
    public void write(SessionHandlerContext ctx, Object msg) throws Exception {
        channel.write(msg, channel.voidPromise());
    }

    @Override
    public void flush(SessionHandlerContext ctx) throws Exception {
        channel.flush();
    }

    @Override
    public void close(SessionHandlerContext ctx, Promise<?> promise) throws Exception {
        channel.close().addListener(future -> promise.trySuccess(null));
    }
}
