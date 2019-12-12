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

package com.wjybxx.fastjgame.misc;

import com.wjybxx.fastjgame.gameobject.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * 玩家消息处理函数注册器的默认实现
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/26
 * github - https://github.com/hl845740757
 */
public class DefaultPlayerEventDispatcher implements PlayerEventHandlerRegistry, PlayerEventDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(DefaultPlayerEventDispatcher.class);

    /**
     * 类型到处理器的映射。
     */
    private final Map<Class<?>, PlayerEventHandler<?>> handlerMap = new IdentityHashMap<>(512);

    @SuppressWarnings("unchecked")
    @Override
    public <T> void register(@Nonnull Class<T> clazz, @Nonnull PlayerEventHandler<T> handler) {
        final PlayerEventHandler<?> existHandler = handlerMap.get(clazz);
        // 该类型目前还没有被注册
        if (existHandler == null) {
            handlerMap.put(clazz, handler);
            return;
        }
        if (existHandler instanceof CompositePlayerEventHandler) {
            ((CompositePlayerEventHandler) existHandler).addHandler(handler);
        } else {
            // 已存在该类型的处理器了，我们提供CompositeMessageHandler将其封装为统一结构
            handlerMap.put(clazz, new CompositePlayerEventHandler(existHandler, handler));
        }
    }

    @Override
    public void release() {
        handlerMap.clear();
    }

    @SuppressWarnings("unchecked")
    @Override
    public final <T> void post(@Nonnull Player player, @Nonnull T event) {
        final PlayerEventHandler<T> messageFunction = (PlayerEventHandler<T>) handlerMap.get(event.getClass());
        if (null == messageFunction) {
            logger.warn("{} send unregistered message {}", player.getGuid(), event.getClass().getName());
            return;
        }
        try {
            messageFunction.onEvent(player, event);
        } catch (Exception e) {
            logger.warn("Handler onEvent caught exception!, handler {}, message {}",
                    messageFunction.getClass().getName(), event.getClass().getName(), e);
        }
    }
}
