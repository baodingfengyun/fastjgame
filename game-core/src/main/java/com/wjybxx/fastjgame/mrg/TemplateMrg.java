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

import com.google.inject.Inject;
import com.wjybxx.fastjgame.config.NpcConfig;
import com.wjybxx.fastjgame.config.PetConfig;
import com.wjybxx.fastjgame.config.SceneConfig;
import com.wjybxx.fastjgame.world.GameEventLoopGroup;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

/**
 * 表格模板控制器，存储着所有的表格信息；
 * 该控制器占用内存较多，不需要的模块不要绑定该控制器。
 * <p>
 * 如果配置全部实现为不可变对象，那么可以作为{@link GameEventLoopGroup}级别的单例。
 * 至少也可以作为线程级别的单例。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/6/23 18:28
 * github - https://github.com/hl845740757
 */
public class TemplateMrg {

    // TODO 读表
    /**
     * 场景配置
     * sceneId -> config
     */
    public final Int2ObjectMap<SceneConfig> sceneConfigInfo = null;

    /**
     * npc配置表
     * npcId -> config
     */
    public final Int2ObjectMap<NpcConfig> npcConfigInfo = null;

    /**
     * 宠物配置表
     * petId -> config
     */
    public final Int2ObjectMap<PetConfig> petConfigInfo = null;

    @Inject
    public TemplateMrg() {

    }

}
