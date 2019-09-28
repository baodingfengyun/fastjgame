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

package com.wjybxx.fastjgame.net;

import com.wjybxx.fastjgame.timer.TimerSystem;
import com.wjybxx.fastjgame.timer.TimerTask;

/**
 * {@link io.netty.channel.ChannelHandler}
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/9/25
 * github - https://github.com/hl845740757
 */
public interface SessionHandler {

    /**
     * 当{@link SessionHandler}成功添加到{@link SessionPipeline}时进行必要的初始化。
     *
     * @param ctx handler所属的context
     */
    void init(SessionHandlerContext ctx) throws Exception;

    /**
     * 刷帧。
     *
     * @param ctx handler所属的context
     * @apiNote 不允许在tick的时候关闭session，如果需要关闭，请使用{@link TimerSystem#nextTick(TimerTask)}下一帧关闭。
     */
    void tick(SessionHandlerContext ctx) throws Exception;
}
