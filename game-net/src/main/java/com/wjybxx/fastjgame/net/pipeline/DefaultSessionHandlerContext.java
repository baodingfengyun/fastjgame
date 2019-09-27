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

package com.wjybxx.fastjgame.net.pipeline;

import com.wjybxx.fastjgame.manager.NetManagerWrapper;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2019/9/25
 * github - https://github.com/hl845740757
 */
class DefaultSessionHandlerContext extends AbstractSessionHandlerContext {

    private final SessionHandler handler;

    /**
     * 是否是入站处理器。
     * 最新版的Netty，这个已经进行了优化，判断handler重写了哪些方法，然后直接找到下一个重写了指定方法的handler，
     * 不但可以有更短的事件流，而且可以大幅减少匿名内部类对象(lambda表达式也是一样)。
     * 我们暂时不必如此。
     */
    private final boolean inbound;
    /**
     * 是否是出站处理器
     */
    private final boolean outbound;

    DefaultSessionHandlerContext(DefaultSessionPipeline pipeline, NetManagerWrapper netManagerWrapper, SessionHandler handler) {
        super(pipeline, netManagerWrapper);
        this.handler = handler;
        this.inbound = handler instanceof SessionInboundHandler;
        this.outbound = handler instanceof SessionOutboundHandler;
    }

    @Override
    public SessionHandler handler() {
        return handler;
    }

    @Override
    public boolean isInbound() {
        return inbound;
    }

    @Override
    public boolean isOutbound() {
        return outbound;
    }
}
