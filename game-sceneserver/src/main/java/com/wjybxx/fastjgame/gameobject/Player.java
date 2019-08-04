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

package com.wjybxx.fastjgame.gameobject;

import com.wjybxx.fastjgame.misc.PlatformType;
import com.wjybxx.fastjgame.net.Session;
import com.wjybxx.fastjgame.scene.gameobjectdata.GameObjectType;
import com.wjybxx.fastjgame.scene.gameobjectdata.PlayerData;

import javax.annotation.Nonnull;

/**
 * 玩家对象，也是机器人对象；
 * 暂时先直接继承GameObject；
 * @author wjybxx
 * @version 1.0
 * date - 2019/6/4 16:58
 * github - https://github.com/hl845740757
 */
public class Player extends GameObject<PlayerData>{

    /**
     * player的一些与场景无关的数据
     */
    private final PlayerData playerData;

    /**
     * 玩家身上的session
     */
    private Session session;

    public Player(PlayerData playerData) {
        this.playerData = playerData;
    }

    @Nonnull
    @Override
    public PlayerData getData() {
        return playerData;
    }

    // --------------这三个是非常常用的属性，因此进行一下代理----------------------------

    /**
     * 玩家所属的运营平台
     */
    public PlatformType getPlatformType() {
        return playerData.getPlatformType();
    }

    /**
     * 玩家注册账号时的服务器id。
     * 是玩家的逻辑服，它并不一定是一个真实的服务器。
     */
    public int getLogicServerId() {
        return playerData.getLogicServerId();
    }

    public int getActualServerId() {
        return playerData.getActualServerId();
    }

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }
}
