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

import javax.annotation.Nonnull;

/**
 * 玩家消息分发器
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/27
 * github - https://github.com/hl845740757
 */
public interface PlayerMessageDispatcher {
    /**
     * 接收到一个玩家发来的消息
     *
     * @param player  消息所在的会话
     * @param message 消息内容
     * @param <T>     消息类型
     */
    <T extends AbstractMessage> void post(@Nonnull Player player, @Nonnull T message);
}
