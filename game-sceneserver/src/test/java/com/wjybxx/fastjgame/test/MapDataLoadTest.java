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

package com.wjybxx.fastjgame.test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.wjybxx.fastjgame.misc.ViewGridSet;
import com.wjybxx.fastjgame.module.WorldGroupModule;
import com.wjybxx.fastjgame.module.SceneModule;
import com.wjybxx.fastjgame.mgr.MapDataLoadMgr;
import com.wjybxx.fastjgame.scene.MapData;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2019/6/5 15:48
 * github - https://github.com/hl845740757
 */
public class MapDataLoadTest {

    public static void main(String[] args) {

        Injector injector = Guice.createInjector(new WorldGroupModule(), new SceneModule());

        MapDataLoadMgr mapDataLoadMgr = injector.getInstance(MapDataLoadMgr.class);

        MapData mapData = mapDataLoadMgr.loadMapData(1);
        ViewGridSet viewGridSet = new ViewGridSet(mapData.getMapWidth(), mapData.getMapHeight(), 600);
        System.out.println(mapData);
    }
}
