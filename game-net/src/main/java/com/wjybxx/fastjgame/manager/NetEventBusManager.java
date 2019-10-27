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

package com.wjybxx.fastjgame.manager;

import com.google.inject.Inject;
import com.wjybxx.fastjgame.eventbus.EventBus;
import com.wjybxx.fastjgame.eventbus.EventDispatcher;
import com.wjybxx.fastjgame.eventbus.EventHandler;
import com.wjybxx.fastjgame.eventbus.EventHandlerRegistry;
import com.wjybxx.fastjgame.utils.ConcurrentUtils;

import javax.annotation.Nonnull;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2019/10/26
 * github - https://github.com/hl845740757
 */
public class NetEventBusManager implements EventHandlerRegistry, EventDispatcher {

    private final EventBus eventBus = new EventBus();
    private final NetEventLoopManager eventLoopManager;

    @Inject
    public NetEventBusManager(NetEventLoopManager eventLoopManager) {
        this.eventLoopManager = eventLoopManager;
    }

    @Override
    public <T> void post(@Nonnull T event) {
        eventBus.post(event);
    }

    @Override
    public <T> void post(Class<? super T> keyClazz, @Nonnull T event) {
        eventBus.post(keyClazz, event);
    }

    @Override
    public <T> void register(@Nonnull Class<T> eventType, @Nonnull EventHandler<T> handler) {
        // 避免在错误的时间调用
        ConcurrentUtils.ensureInEventLoop(eventLoopManager.getEventLoop());
        eventBus.register(eventType, handler);
    }

    @Override
    public void release() {
        eventBus.release();
    }
}
