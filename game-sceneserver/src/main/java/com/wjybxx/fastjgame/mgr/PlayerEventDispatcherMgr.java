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

package com.wjybxx.fastjgame.mgr;

import com.google.inject.Inject;
import com.wjybxx.fastjgame.eventbus.EventBus;
import com.wjybxx.fastjgame.eventbus.EventHandler;
import com.wjybxx.fastjgame.eventbus.EventHandlerRegistry;
import com.wjybxx.fastjgame.gameobject.Player;

import javax.annotation.Nonnull;

/**
 * 玩家事件分发器
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/12/19
 * github - https://github.com/hl845740757
 */
public class PlayerEventDispatcherMgr implements EventHandlerRegistry {

    private final EventBus eventBus = new EventBus(1024);

    @Inject
    public PlayerEventDispatcherMgr() {
    }

    public <E> void post(@Nonnull Player player, @Nonnull E event) {
        eventBus.post(player, event);
    }

    @Override
    public <T, E> void register(@Nonnull Class<E> eventType, @Nonnull EventHandler<T, ? super E> handler) {
        eventBus.register(eventType, handler);
    }

    @Override
    public void release() {
        eventBus.release();
    }
}
