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

import com.wjybxx.fastjgame.net.Session;

/**
 * {@link SessionPipeline}入站事件处理器。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/9/25
 * github - https://github.com/hl845740757
 */
public interface SessionInboundHandler extends SessionHandler {

    /**
     * 当session激活时。
     * 注意：此时{@link Session#isActive() true}
     *
     * @param ctx handler所处的上下文
     */
    void onSessionActive(SessionHandlerContext ctx) throws Exception;

    /**
     * 当session被关闭时。
     * 注意：此时{@link Session#isActive() false}
     *
     * @param ctx handler所处的上下文
     */
    void onSessionInactive(SessionHandlerContext ctx) throws Exception;

    /**
     * 读取一个消息。
     * 注意：此时{@link Session#isActive() true}
     * <p>
     * Q: 为什么没有定义成三个方法？
     * A: 这就是很坑的一点了，因为即使是同一个事件，每一层需要的参数也不一样，所以只能使用Object了！
     * 代码要易扩展，性能就有损失。
     *
     * @param ctx handler所处的上下文
     * @param msg 前一个handler转来的消息对象
     */
    void read(SessionHandlerContext ctx, Object msg);

    /**
     * 当某一个handler处理事件出现异常时。
     *
     * @param ctx   handler 所属的上下文
     * @param cause 原因
     */
    void onExceptionCaught(SessionHandlerContext ctx, Throwable cause) throws Exception;

}
