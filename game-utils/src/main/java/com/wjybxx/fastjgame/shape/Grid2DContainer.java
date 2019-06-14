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

package com.wjybxx.fastjgame.shape;

import com.wjybxx.fastjgame.utils.MathUtils;

/**
 * 2D格子容器
 * @author wjybxx
 * @version 1.0
 * @date 2019/6/11 17:08
 * @github - https://github.com/hl845740757
 */
public interface Grid2DContainer<T extends Grid2D> {

    T[][] getAllGrids();
    
    default int getRowCount(){
        return getAllGrids().length;
    }
    
    default int getColCount(){
        return getAllGrids()[0].length;
    }

    /**
     * 检查格子索引是否在容器内
     * (数学计算)
     * @param x 列索引
     * @param y 行索引
     * @return
     */
    default boolean inside(int x, int y){
        return MathUtils.withinRange(0, getColCount(), x) &&
                MathUtils.withinRange(0, getRowCount(), y);
    }

    /**
     * 获取指定行列位置格子。
     * 使用前请调用{@link #inside(int, int)}检查是否合法；
     * (数学计算)
     * @param x 列索引
     * @param y 行索引
     */
    default T getGrid(int x, int y){
        return getAllGrids()[y][x];
    }

    /**
     * 检查格子索引是否在容器内
     * (debug)
     * @param rowIndex 行索引
     * @param colIndex 列索引
     * @return
     */
    default boolean inside2(int rowIndex, int colIndex){
        return MathUtils.withinRange(0, getRowCount(), rowIndex)&&
                MathUtils.withinRange(0, getColCount(), colIndex);
    }

    /**
     * 获取指定行列位置格子。
     * 使用前请调用{@link #inside2(int, int)}检查是否合法;
     * (debug)
     * @param rowIndex 行索引
     * @param colIndex 列索引
     */
    default T getGrid2(int rowIndex, int colIndex){
        return getAllGrids()[rowIndex][colIndex];
    }
}
