/*
 * Copyright 2019 wjybxx
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wjybxx.fastjgame.mrg;

import com.wjybxx.fastjgame.gameobject.Player;
import com.wjybxx.fastjgame.misc.PlayerSession;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * 管理与玩家之间的连接
 * @author wjybxx
 * @version 1.0
 * date - 2019/6/23 0:10
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public class PlayerSessionMrg {

    /**
     * playerGuid -> session
     */
    private final Long2ObjectMap<PlayerSession> guid2SessionMap = new Long2ObjectOpenHashMap<>();

    /**
     * 玩家是否登录scene进程成功了，玩家连接上scene这个事件是请求登录scene。
     * 必须等到center服的验证结果之后，玩家数据才会从center到scene。
     * @param playerGuid 玩家guid
     * @return true/false
     */
    public boolean isLoginSuccess(long playerGuid){
        return guid2SessionMap.containsKey(playerGuid);
    }

    public PlayerSession getPlayerSession(long playerGuid){
        return guid2SessionMap.get(playerGuid);
    }

    /**
     * 当玩家断开连接
     * @param playerGuid
     */
    public void onPlayerDisconnect(long playerGuid){
        // TODO 处理玩家掉线的情况
    }

    // TODO 处理玩家请求进入场景的消息
}
