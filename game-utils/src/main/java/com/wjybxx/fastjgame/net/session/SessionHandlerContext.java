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

import com.wjybxx.fastjgame.net.eventloop.NetEventLoop;
import com.wjybxx.fastjgame.utils.concurrent.EventLoop;
import com.wjybxx.fastjgame.utils.timer.TimerSystem;

/**
 * {@link SessionInboundHandler} {@link SessionOutboundHandler}的运行环境。
 * <p>
 * 它主要负责组织{@link SessionPipeline}中的{@link SessionInboundHandler}{@link SessionOutboundHandler}。
 * 构成了职责链模式（或者说过滤拦截器模式）。
 * </p>
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/9/25
 * github - https://github.com/hl845740757
 */
public interface SessionHandlerContext extends SessionInboundInvoker, SessionOutboundInvoker {

    /**
     * @return 该context所属的session
     */
    Session session();

    /**
     * @return session的pipeline
     */
    SessionPipeline pipeline();

    /**
     * @return session所在的netEventLoop
     */
    NetEventLoop netEventLoop();

    /**
     * @return session所在的逻辑线程
     */
    EventLoop appEventLoop();

    /**
     * @return pipeline私有的定时器系统，在session关闭后停止运行。
     */
    TimerSystem timerSystem();

    /**
     * @return 该context管理的handler。
     */
    SessionHandler handler();

    /**
     * 调用{@link SessionHandler#handlerAdded(SessionHandlerContext)}方法。
     */
    void handlerAdded();

    /**
     * 调用{@link SessionHandler#handlerRemoved(SessionHandlerContext)} 方法。
     */
    void handlerRemoved();

    /**
     * 刷帧
     */
    void tick();
}
