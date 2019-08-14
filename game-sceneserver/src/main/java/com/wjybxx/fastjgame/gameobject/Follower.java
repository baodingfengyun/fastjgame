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

import com.wjybxx.fastjgame.scene.Scene;
import com.wjybxx.fastjgame.scene.gameobjectdata.GameObjectData;
import com.wjybxx.fastjgame.trigger.SystemTimeProvider;

import javax.annotation.Nullable;

/**
 * 跟随单位
 * @author wjybxx
 * @version 1.0
 * date - 2019/6/23 17:08
 * github - https://github.com/hl845740757
 */
public abstract class Follower<T extends GameObjectData> extends GameObject<T>{

    protected GameObject owner;

    public Follower(Scene scene, SystemTimeProvider timeProvider, GameObject owner) {
        super(scene, timeProvider);
        this.owner = owner;
    }

    @Nullable
    public final GameObject getOwner() {
        return owner;
    }

    /**
     * 获取宠物主人的guid，如果不存在主人，则返回-1
     * @return
     */
    public final long getOwnerGuid() {
        return null == owner ? -1 : owner.getGuid();
    }

}
