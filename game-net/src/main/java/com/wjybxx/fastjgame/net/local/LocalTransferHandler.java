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

import com.wjybxx.fastjgame.concurrent.Promise;
import com.wjybxx.fastjgame.net.session.Session;
import com.wjybxx.fastjgame.net.session.SessionHandlerContext;
import com.wjybxx.fastjgame.net.session.SessionOutboundHandlerAdapter;

/**
 * JVM 内部传输实现 - 它是出站的最后一个处理器，因此也是真正实现关闭的handler
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/9/26
 * github - https://github.com/hl845740757
 */
public class LocalTransferHandler extends SessionOutboundHandlerAdapter {

    private Session remoteSession;

    @Override
    public void init(SessionHandlerContext ctx) throws Exception {
        remoteSession = ((LocalSessionImp) (ctx.session())).getRemoteSession();
    }

    @Override
    public void write(SessionHandlerContext ctx, Object msg) throws Exception {
        // 直接触发另一个session的读事件
        remoteSession.fireRead(msg);
    }

    @Override
    public void close(SessionHandlerContext ctx, Promise<?> promise) throws Exception {
        // 标记为成功
        promise.trySuccess(null);
        // 减少不必要的调用
        if (ctx.managerWrapper().getSessionManager().containsSession(remoteSession)) {
            remoteSession.close();
        }
    }
}
