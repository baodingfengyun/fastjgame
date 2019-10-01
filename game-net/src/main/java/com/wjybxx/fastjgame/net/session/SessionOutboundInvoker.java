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

import javax.annotation.Nonnull;

/**
 * {@link SessionPipeline}出站事件调度器。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/9/25
 * github - https://github.com/hl845740757
 */
public interface SessionOutboundInvoker {

    /**
     * 向下传递发送消息请求；
     * 它将导致{@link SessionPipeline}中的下一个{@link SessionOutboundHandler#write(SessionHandlerContext, Object)} 方法被调用。
     *
     * @param msg 消息内容
     */
    void fireWrite(@Nonnull Object msg);

    /**
     * 向下传递清空缓冲区请求；
     * 它将导致{@link SessionPipeline}中的下一个{@link SessionOutboundHandler#flush(SessionHandlerContext)}方法被调用。
     */
    void fireFlush();

    /**
     * {@link #fireWrite(Object)}和{@link #fireFlush()}的一个快捷调用方式
     */
    void fireWriteAndFlush(@Nonnull Object msg);

    /**
     * 向下发送关闭session请求；
     * 它将导致{@link SessionPipeline}中的下一个{@link SessionOutboundHandler#close(SessionHandlerContext, Promise)} 方法被调用。
     *
     * @param promise 用于获取结果的future
     */
    void fireClose(Promise<?> promise);

}
