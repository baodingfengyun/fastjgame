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

import com.wjybxx.fastjgame.config.PetConfig;
import com.wjybxx.fastjgame.scene.gameobjectdata.GameObjectData;
import com.wjybxx.fastjgame.scene.gameobjectdata.GameObjectType;

import javax.annotation.Nonnull;

/**
 * 宠物的与场景无关的数据
 * @author wjybxx
 * @version 1.0
 * date - 2019/6/23 0:26
 * github - https://github.com/hl845740757
 */
public class PetData extends GameObjectData {

    private final PetConfig config;

    public PetData(long guid, PetConfig config) {
        super(guid);
        this.config = config;
    }

    @Nonnull
    @Override
    public GameObjectType getObjectType() {
        return GameObjectType.PET;
    }

    public PetConfig getConfig() {
        return config;
    }

    public int getPetId() {
        return config.petId;
    }
}
