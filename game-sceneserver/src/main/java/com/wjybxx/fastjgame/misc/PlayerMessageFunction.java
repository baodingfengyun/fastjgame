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

/**
 * 玩家消息处理函数，用于代码生成。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/25
 * github - https://github.com/hl845740757
 */
@FunctionalInterface
public interface PlayerMessageFunction<T extends AbstractMessage> {

    /**
     * 当接收到一个玩家的消息
     *
     * @param player  发送消息的玩家
     * @param message 玩家发送的消息
     */
    void onMessage(Player player, T message);
}
