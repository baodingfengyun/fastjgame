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
import com.wjybxx.fastjgame.eventbus.GenericEvent;
import com.wjybxx.fastjgame.misc.PlayerEvent;
import com.wjybxx.fastjgame.misc.PlayerMsgEvent;

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

    public <T> void post(PlayerEvent<T> playerEvent) {
        eventBus.post(playerEvent);
    }

    @Override
    public <E> void register(@Nonnull Class<E> eventType, @Nonnull EventHandler<? super E> handler) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T extends GenericEvent<U>, U> void register(@Nonnull Class<T> genericType, Class<U> childType, @Nonnull EventHandler<? super T> handler) {
        if (!PlayerEvent.class.isAssignableFrom(genericType)) {
            throw new UnsupportedOperationException();
        }
        if (PlayerMsgEvent.class.isAssignableFrom(genericType)) {
            return;
        }
        // 注册消息处理器以外的
        eventBus.register(genericType, childType, handler);
    }

    @Override
    public void release() {
        eventBus.release();
    }
}
