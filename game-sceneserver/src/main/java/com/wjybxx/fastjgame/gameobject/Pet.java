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
import com.wjybxx.fastjgame.scene.Scene;
import com.wjybxx.fastjgame.timeprovider.TimeProvider;

import javax.annotation.Nonnull;

/**
 * 宠物对象
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/6/4 16:59
 * github - https://github.com/hl845740757
 */
public class Pet extends Follower<PetData> {

    private final PetData petData;

    public Pet(Scene scene, TimeProvider timeProvider, GameObject owner, PetData petData) {
        super(scene, timeProvider, owner);
        this.petData = petData;
    }

    @Nonnull
    @Override
    public PetData getData() {
        return petData;
    }

    public PetConfig getConfig() {
        return petData.getConfig();
    }

    public int getPetId() {
        return petData.getPetId();
    }
}
