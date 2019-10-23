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

package com.wjybxx.fastjgame.module;

import com.google.inject.Singleton;
import com.wjybxx.fastjgame.mgr.*;
import com.wjybxx.fastjgame.world.SceneWorld;
import com.wjybxx.fastjgame.world.World;

/**
 * 场景模块需要绑定的对象(依赖注入管理的对象)
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/16 21:20
 * github - https://github.com/hl845740757
 */
public class SceneModule extends WorldModule {

    @Override
    protected void bindWorldAndWorldInfoMgr() {
        bind(WorldInfoMgr.class).to(SceneWorldInfoMgr.class).in(Singleton.class);
        bind(World.class).to(SceneWorld.class).in(Singleton.class);
        bind(ProtocolDispatcherMgr.class).in(Singleton.class);
    }

    @Override
    protected void bindOthers() {
        bind(SceneWorldInfoMgr.class).in(Singleton.class);
        bind(CenterInSceneInfoMgr.class).in(Singleton.class);
        bind(SceneRegionMgr.class).in(Singleton.class);
        bind(SceneSendMgr.class).in(Singleton.class);
        bind(MapDataLoadMgr.class).in(Singleton.class);
        bind(SceneWrapper.class).in(Singleton.class);
        bind(SceneMgr.class).in(Singleton.class);
        bind(PlayerSessionMgr.class).in(Singleton.class);
        bind(PlayerMessageDispatcherMgr.class).in(Singleton.class);
    }
}
