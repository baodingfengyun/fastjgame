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

import com.wjybxx.fastjgame.net.rpc.NetLogicMessage;
import com.wjybxx.fastjgame.net.serialization.Serializer;
import com.wjybxx.fastjgame.net.session.SessionHandlerContext;
import com.wjybxx.fastjgame.net.session.SessionOutboundHandlerAdapter;

/**
 * 对于在JVM内传输的数据，进行保护性拷贝。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/9/25
 * github - https://github.com/hl845740757
 */
public class LocalCodecHandler extends SessionOutboundHandlerAdapter {

    private Serializer serializer;

    public LocalCodecHandler() {

    }

    @Override
    public void handlerAdded(SessionHandlerContext ctx) throws Exception {
        serializer = ctx.session().config().serializer();
    }

    @Override
    public void write(SessionHandlerContext ctx, Object msg) throws Exception {
        // msg 是根据writeTask创建的对象，不是共享的，但它持有的内容是共享的
        if (msg instanceof NetLogicMessage) {
            NetLogicMessage logicMessage = (NetLogicMessage) msg;
            final byte[] bodyBytes = serializer.toBytes(logicMessage.getBody());
            final Object newBody = serializer.fromBytes(bodyBytes);
            logicMessage.setBody(newBody);
        }
        // 传递给下一个handler
        ctx.fireWrite(msg);
    }

}
