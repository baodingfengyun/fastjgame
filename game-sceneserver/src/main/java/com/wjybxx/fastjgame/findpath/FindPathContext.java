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

import com.wjybxx.fastjgame.scene.MapData;
import com.wjybxx.fastjgame.scene.MapGrid;

import javax.annotation.Nonnull;

/**
 * 寻路参数，避免大量的传参
 * （参数对象）
 * @author wjybxx
 * @version 1.0
 * date - 2019/6/12 15:35
 * github - https://github.com/hl845740757
 */
public abstract class FindPathContext {

    public final MapData mapData;

    public final MapGrid startGrid;

    public final MapGrid endGrid;

    public final WalkableGridStrategy walkableGridStrategy;

    protected FindPathContext(MapData mapData, MapGrid startGrid, MapGrid endGrid, WalkableGridStrategy walkableGridStrategy) {
        this.mapData = mapData;
        this.startGrid = startGrid;
        this.endGrid = endGrid;
        this.walkableGridStrategy = walkableGridStrategy;
    }

    /**
     * 获取指定坐标的
     * @param x x坐标，列索引
     * @param y y坐标，行索引
     * @return 如果坐标未越界，则返回指定格子，否则返回null
     */
    public MapGrid getGrid(int x, int y){
        if (mapData.inside(x,y)){
            return mapData.getGrid(x, y);
        }
        return null;
    }

    /**
     * 指定格子是否可移动
     * @param mapGrid 地图格子
     * @return
     */
    public boolean isWalkable(@Nonnull MapGrid mapGrid){
        return walkableGridStrategy.walkable(mapGrid.getObstacleValue());
    }

    /**
     * 指定坐标的格子是否可以移动
     * （指定的坐标可能越界）
     * @param x x坐标，列索引
     * @param y y坐标，行索引
     * @return
     */
    public boolean isWalkable(int x, int y){
        return mapData.inside(x, y) && walkableGridStrategy.walkable(mapData.getGrid(x, y).getObstacleValue());
    }

    /**
     * 指定坐标的格子是否在地图内
     * @param x x坐标，列索引
     * @param y y坐标，行索引
     * @return
     */
    public boolean isInside(int x, int y){
        return mapData.inside(x, y);
    }

    /**
     * 是否是目标格子
     * @param x x坐标，列索引
     * @param y y坐标，行索引
     * @return
     */
    public boolean isEndGrid(int x, int y){
        return x == endGrid.getX() && y == endGrid.getY();
    }
}
