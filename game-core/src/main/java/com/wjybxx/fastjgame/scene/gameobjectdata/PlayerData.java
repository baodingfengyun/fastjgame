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

package com.wjybxx.fastjgame.scene.gameobjectdata;

import com.wjybxx.fastjgame.misc.CenterServerId;

import javax.annotation.Nonnull;

/**
 * 玩家数据
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/6/23 0:19
 * github - https://github.com/hl845740757
 */
public class PlayerData extends GameObjectData {
    /**
     * 玩家注册账号时的服务器id。
     * 是玩家的逻辑服，它并不一定是一个真实的服务器。
     */
    private CenterServerId originalServerId;

    /**
     * 玩家当前真正所属的服务器（合服之后的服）
     */
    private CenterServerId actualServerId;

    public PlayerData(long guid) {
        super(guid);
    }

    @Nonnull
    @Override
    public GameObjectType getObjectType() {
        return GameObjectType.PLAYER;
    }

    public CenterServerId getOriginalServerId() {
        return originalServerId;
    }

    public void setOriginalServerId(CenterServerId originalServerId) {
        this.originalServerId = originalServerId;
    }

    public CenterServerId getActualServerId() {
        return actualServerId;
    }

    public void setActualServerId(CenterServerId actualServerId) {
        this.actualServerId = actualServerId;
    }
}
