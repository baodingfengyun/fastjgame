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

package com.wjybxx.fastjgame.net.session;

import com.wjybxx.fastjgame.concurrent.Promise;


/**
 * {@link SessionOutboundHandler}的适配器，默认将事件传递给{@link SessionPipeline}的下一个{@link SessionOutboundHandler}.
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/9/25
 * github - https://github.com/hl845740757
 */
public class SessionOutboundHandlerAdapter extends SessionHandlerAdapter implements SessionOutboundHandler {

    @Override
    public void write(SessionHandlerContext ctx, Object msg) throws Exception {
        ctx.fireWrite(msg);
    }

    @Override
    public void flush(SessionHandlerContext ctx) throws Exception {
        ctx.fireFlush();
    }

    @Override
    public void close(SessionHandlerContext ctx, Promise<?> promise) throws Exception {
        ctx.fireClose(promise);
    }
}
