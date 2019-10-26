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

package com.wjybxx.fastjgame.concurrent.event;

import com.wjybxx.fastjgame.concurrent.EventLoop;
import com.wjybxx.fastjgame.eventbus.EventBus;
import com.wjybxx.fastjgame.eventbus.EventDispatcher;
import com.wjybxx.fastjgame.eventbus.EventHandler;
import com.wjybxx.fastjgame.eventbus.EventHandlerRegistry;
import com.wjybxx.fastjgame.utils.ConcurrentUtils;

import javax.annotation.Nonnull;

/**
 * {@link EventBus}的代理，实现线程安全
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/10/26
 * github - https://github.com/hl845740757
 */
public class EventLoopEventBus implements EventHandlerRegistry, EventDispatcher {

    private final EventLoop eventLoop;

    private final EventBus eventBus;

    public EventLoopEventBus(EventLoop eventLoop, EventBus eventBus) {
        this.eventLoop = eventLoop;
        this.eventBus = eventBus;
    }


    @Override
    public <T> void post(@Nonnull T event) {
        if (eventLoop.inEventLoop()) {
            // 当前处于事件循环线程，直接提交事件
            eventBus.post(event);
        } else {
            // 提交到事件循环线程
            eventLoop.execute(new EventBusTask(eventBus, event));
        }
    }

    @Override
    public <T> void register(@Nonnull Class<T> eventType, @Nonnull EventHandler<T> handler) {
        ConcurrentUtils.ensureInEventLoop(eventLoop);
        eventBus.register(eventType, handler);
    }

    @Override
    public void release() {
        ConcurrentUtils.ensureInEventLoop(eventLoop);
        eventBus.release();
    }


}
