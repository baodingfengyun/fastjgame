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
import com.wjybxx.fastjgame.manager.NetManagerWrapper;
import io.netty.channel.ChannelPipeline;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.NoSuchElementException;

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
     * 获取所属的{@link NetEventLoop}内的所有管理器
     *
     * @return NetManagerWrapper
     */
    NetManagerWrapper managerWrapper();

    /**
     * 添加一个handler到pipeline的尾部
     *
     * @param handler handler
     * @return this
     */
    SessionPipeline addLast(@Nonnull SessionHandler handler);

    /**
     * 添加一个handler到pipeline的头部
     *
     * @param handler handler
     * @return this
     */
    SessionPipeline addFirst(@Nonnull SessionHandler handler);

    /**
     * 移除pipeline中的第一个{@link SessionHandler}
     *
     * @return 成功移除的handler
     * @throws NoSuchElementException 如果管道中为空，则抛出该异常
     */
    SessionHandler removeFirst();

    /**
     * 移除pipeline中的最后一个{@link SessionHandler}
     *
     * @return 成功移除的handler
     * @throws NoSuchElementException 如果管道中为空，则抛出该异常
     */
    SessionHandler removeLast();

    /**
     * 从管道中移除指定{@link SessionHandler}
     *
     * @return this
     * @throws NoSuchElementException 如果管道中不存在该handler，则抛出该异常
     */
    SessionPipeline remove(@Nonnull SessionHandler handler);

    /**
     * @return pipeline中的第一个handler，如果pipeline为空，则返回null
     */
    @Nullable
    SessionHandler firstHandler();

    /**
     * @return pipeline中的最后一个handler，如果pipeline为空，则返回null
     */
    @Nullable
    SessionHandler lastHandler();

    /**
     * @return 返回pipeline中的第一个handler的context，如果pipeline为空，则返回null
     */
    @Nullable
    SessionHandlerContext firstContext();

    /**
     * @return 返回pipeline中的最后一个handler的context，如果pipeline为空，则返回null
     */
    @Nullable
    SessionHandlerContext lastContext();

    /**
     * @param handler 要查找的handler
     * @return handler对应的context，如果不存在则返回null
     */
    @Nullable
    SessionHandlerContext context(SessionHandler handler);

    /**
     * 刷帧，调用每一个handler的{@link SessionHandler#tick(SessionHandlerContext)}方法
     */
    void fireTick();

}
