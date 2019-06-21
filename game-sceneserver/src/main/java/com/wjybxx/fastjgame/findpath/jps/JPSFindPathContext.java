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

package com.wjybxx.fastjgame.findpath.jps;

import com.wjybxx.fastjgame.findpath.FindPathContext;
import com.wjybxx.fastjgame.findpath.WalkableGridStrategy;
import com.wjybxx.fastjgame.scene.MapData;
import com.wjybxx.fastjgame.scene.MapGrid;

/**
 * jps寻路上下文
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/6/12 16:16
 * github - https://github.com/hl845740757
 */
public class JPSFindPathContext extends FindPathContext {

    public JPSFindPathContext(MapData mapData, MapGrid startGrid, MapGrid endGrid, WalkableGridStrategy walkableGridStrategy) {
        super(mapData, startGrid, endGrid, walkableGridStrategy);
    }

}
