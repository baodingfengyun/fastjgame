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

package com.wjybxx.fastjgame.misc;

import com.wjybxx.fastjgame.gameobject.Player;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * 玩家与scene进程的会话信息管理
 * @author wjybxx
 * @version 1.0
 * date - 2019/6/23 0:11
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public class PlayerSession {

    /**
     * 玩家数据
     */
    private final Player player;

    /**
     * 玩家当前所在场景
     */
    private long sceneGuid;

    public PlayerSession(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    public long getSceneGuid() {
        return sceneGuid;
    }

    public void setSceneGuid(long sceneGuid) {
        this.sceneGuid = sceneGuid;
    }
}
