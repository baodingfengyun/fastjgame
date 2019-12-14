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
import java.util.ArrayList;
import java.util.List;

/**
 * 为多个同类型的事件处理函数提供一个单一的视图。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/25
 * github - https://github.com/hl845740757
 */
public class CompositePlayerEventHandler<T> implements PlayerEventHandler<T> {

    private static final Logger logger = LoggerFactory.getLogger(CompositePlayerEventHandler.class);

    /**
     * 一般情况下，玩家的消息都是一对一的，即使出现多个订阅者，也从很小的容量开始扩增。
     */
    private final List<PlayerEventHandler<T>> children = new ArrayList<>(2);

    public CompositePlayerEventHandler() {
    }

    public CompositePlayerEventHandler(PlayerEventHandler<T> first, PlayerEventHandler<T> second) {
        children.add(first);
        children.add(second);
    }

    public CompositePlayerEventHandler addHandler(PlayerEventHandler<T> function) {
        children.add(function);
        return this;
    }

    @Override
    public void onEvent(Player player, @Nonnull T event) {
        for (PlayerEventHandler<T> function : children) {
            try {
                function.onEvent(player, event);
            } catch (Throwable e) {
                logger.warn("Child onEvent caught exception, playerGuid = {}, message = {}",
                        player.getGuid(), event.getClass().getName(), e);
            }
        }
    }
}
