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

import com.wjybxx.fastjgame.config.NpcConfig;
import com.wjybxx.fastjgame.scene.gameobjectdata.GameObjectData;
import com.wjybxx.fastjgame.scene.gameobjectdata.GameObjectType;

import javax.annotation.Nonnull;

/**
 * npc的与场景无关的数据
 * @author wjybxx
 * @version 1.0
 * date - 2019/6/23 0:24
 * github - https://github.com/hl845740757
 */
public class NpcData extends GameObjectData {

    private final NpcConfig config;

    public NpcData(long guid, @Nonnull NpcConfig config) {
        super(guid);
        this.config = config;
    }

    @Nonnull
    @Override
    public GameObjectType getObjectType() {
        return GameObjectType.NPC;
    }

    @Nonnull
    public NpcConfig getConfig() {
        return config;
    }

    public int getNpcId(){
        return config.npcId;
    }
}
