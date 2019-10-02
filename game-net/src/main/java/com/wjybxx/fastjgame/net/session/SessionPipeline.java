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

import com.wjybxx.fastjgame.concurrent.EventLoop;
import com.wjybxx.fastjgame.eventloop.NetEventLoop;
import io.netty.channel.ChannelPipeline;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * 其意义可参考{@link ChannelPipeline}。
 * 出站方向和入站方向和{@link ChannelPipeline}保持一致。
 * <p>
 * 它不是线程安全的 - 它只会在{@link NetEventLoop}中使用它，线程封闭以消除不必要的同步操作。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/9/25
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public interface SessionPipeline extends SessionInboundInvoker, SessionOutboundInvoker {

    /**
     * @return pipeline所属的session
     */
    Session session();

    /**
     * @return session所在的netEventLoop
     */
    NetEventLoop netEventLoop();

    /**
     * @return session所在的逻辑线程
     */
    EventLoop localEventLoop();

    /**
     * 添加一个handler到pipeline的尾部
     *
     * @param handler handler
     * @return this
     */
    SessionPipeline addLast(SessionHandler handler);

    /**
     * 添加一个handler到pipeline的头部
     *
     * @param handler handler
     * @return this
     */
    SessionPipeline addFirst(SessionHandler handler);

    /**
     * 将调用每一个handler的{@link SessionHandler#init(SessionHandlerContext)}方法。
     */
    SessionPipeline fireInit();

    /**
     * 刷帧
     */
    void tick();

}
