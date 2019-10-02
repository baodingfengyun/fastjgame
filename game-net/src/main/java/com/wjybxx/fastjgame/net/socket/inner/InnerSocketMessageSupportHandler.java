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

import com.wjybxx.fastjgame.net.common.NetMessage;
import com.wjybxx.fastjgame.net.session.SessionDuplexHandlerAdapter;
import com.wjybxx.fastjgame.net.session.SessionHandlerContext;
import com.wjybxx.fastjgame.net.socket.SocketMessageEvent;
import com.wjybxx.fastjgame.net.socket.SocketMessageTO;

/**
 * 内网服务器之间使用的消息支持 - 不校验ack和sequence
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/10/1
 * github - https://github.com/hl845740757
 */
public class InnerSocketMessageSupportHandler extends SessionDuplexHandlerAdapter {

    @Override
    public void read(SessionHandlerContext ctx, Object msg) {
        if (msg instanceof SocketMessageEvent) {
            // 读取有效内容 - 不校验ack和sequence
            ctx.fireRead(((SocketMessageEvent) msg).getWrappedMessage());
        } else {
            ctx.fireRead(msg);
        }
    }

    @Override
    public void write(SessionHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof NetMessage) {
            // 内网不使用真正的ack和sequence
            final SocketMessageTO socketMessageTO = new InnerSocketMessage((NetMessage) msg);
            ctx.fireWrite(socketMessageTO);
        } else {
            ctx.fireWrite(msg);
        }
    }
}
