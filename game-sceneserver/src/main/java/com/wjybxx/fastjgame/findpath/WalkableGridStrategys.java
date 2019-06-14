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

package com.wjybxx.fastjgame.findpath;

import com.wjybxx.fastjgame.scene.GridObstacle;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

/**
 * @author wjybxx
 * @version 1.0
 * @date 2019/6/12 19:18
 * @github - https://github.com/hl845740757
 */
public class WalkableGridStrategys {

    /**
     * npc可以行走的格子
     */
    public static final WalkableGridStrategy npcWalkableGrids = except(GridObstacle.OBSTACLE);

    /**
     * 玩家可以行走的格子
     */
    public static final WalkableGridStrategy playerWalkableGrids = except(GridObstacle.OBSTACLE);

    /**
     * 任意格子都可以行走
     */
    public static final WalkableGridStrategy anyGrids = except();

    /**
     * 只有指定格子可以走，其它格子都不可以走
     * @param walkableGrids 可以走动的格子
     */
    public static WalkableGridStrategy valueOf(@Nonnull GridObstacle...walkableGrids){
        EnumSet<GridObstacle> walkableGridSet = EnumSet.copyOf(Arrays.asList(walkableGrids));
        return new DefaultWalkableGridStrategy(walkableGridSet);
    }

    /**
     * 除了指定格子类型不可走之外，其它格子都可以走
     * @param unwalkableGrids 不可以走的格子
     */
    public static WalkableGridStrategy except(@Nonnull GridObstacle...unwalkableGrids){
        EnumSet<GridObstacle> walkableGridSet = EnumSet.allOf(GridObstacle.class);
        List<GridObstacle> unwalkableGridList = Arrays.asList(unwalkableGrids);
        walkableGridSet.removeAll(unwalkableGridList);
        return new DefaultWalkableGridStrategy(walkableGridSet);
    }

    /**
     * 实现参考{@code java.util.RegularEnumSet}
     * 遮挡标记值数量肯定不会大于64的
     */
    private static class DefaultWalkableGridStrategy implements WalkableGridStrategy{

        private long elements;

        private DefaultWalkableGridStrategy(EnumSet<GridObstacle> walkableGridSet) {
            for (GridObstacle e: walkableGridSet){
                elements |= (1L << e.ordinal());
            }
        }

        @Override
        public boolean walkable(GridObstacle e) {
            return (elements & (1L << e.ordinal())) != 0;
        }
    }
}
