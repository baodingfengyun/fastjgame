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

import com.google.protobuf.AbstractMessage;
import com.wjybxx.fastjgame.gameobject.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 为多个同类型的消息处理函数提供一个单一的视图。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/25
 * github - https://github.com/hl845740757
 */
public class CompositePlayerMessageFunction<T extends AbstractMessage> implements PlayerMessageFunction<T>{

    private static final Logger logger = LoggerFactory.getLogger(CompositePlayerMessageFunction.class);

    /**
     * 一般情况下，玩家的消息都是一对一的，即使出现多个订阅者，也从很小的容量开始扩增。
     */
    private final List<PlayerMessageFunction<T>> children = new ArrayList<>(2);

    public CompositePlayerMessageFunction() {
    }

    public CompositePlayerMessageFunction(PlayerMessageFunction<T> first, PlayerMessageFunction<T> second) {
        children.add(first);
        children.add(second);
    }

    public CompositePlayerMessageFunction addHandler(PlayerMessageFunction<T> function) {
        children.add(function);
        return this;
    }

    @Override
    public void onMessage(Player player, T message) {
        for (PlayerMessageFunction<T> function:children) {
            try {
                function.onMessage(player, message);
            } catch (Exception e) {
                logger.warn("onMessage caught exception, playerGuid = {}, message = {}",
                        player.getGuid(), message.getClass().getName(), e);
            }
        }
    }
}
