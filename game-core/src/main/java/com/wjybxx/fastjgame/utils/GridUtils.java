/*
 *  Copyright 2019 wjybxx
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to iBn writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.wjybxx.fastjgame.utils;

import com.wjybxx.fastjgame.shape.Point2D;

/**
 * 地图格子相关方法
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/12
 * github - https://github.com/hl845740757
 */
public class GridUtils {
    /**
     * 计算格子总行数
     *
     * @param mapHeight  地图高度
     * @param gridHeight 格子高度或宽度（正方形）
     * @return 格子总行数
     */
    public static int rowCount(int mapHeight, int gridHeight) {
        return MathUtils.divideIntCeil(mapHeight, gridHeight);
    }

    /**
     * 计算一个点的行索引；
     * 需要注意越界问题，正方向边界需要-1；
     * 普通格子是不包含右侧边和上侧边的，而最右边格子包含右侧边，最上边格子包含上侧边；
     *
     * @param rowCount   总行数
     * @param gridHeight 格子高度
     * @param y          当前所在y坐标
     * @return 当前y坐标对应的行索引
     */
    public static int rowIndex(int rowCount, int gridHeight, float y) {
        return Math.min(rowCount - 1, (int) y / gridHeight);
    }

    /**
     * 计算格子总列数(进一法)
     *
     * @param mapWidth  地图总宽度
     * @param gridWidth 格子宽度
     * @return 格子总列数
     */
    public static int colCount(int mapWidth, int gridWidth) {
        return MathUtils.divideIntCeil(mapWidth, gridWidth);
    }

    /**
     * 计算一个点的列索引
     *
     * @param colCount  总列数
     * @param gridWidth 格子宽度
     * @param x         当前x坐标
     * @return colIndex
     */
    public static int colIndex(int colCount, int gridWidth, float x) {
        return Math.min(colCount - 1, (int) x / gridWidth);
    }

    /**
     * 格子顶点坐标(针对宽高一样的格子)
     *
     * @param rowIndex  行索引，行索引对应的是Y索引 算出的是Y值
     * @param colIndex  列索引，列索引对应的是X索引 算出的是X值
     * @param gridWidth 格子宽度
     * @return VertexPoint
     */
    public static Point2D gridVertexLocation(int rowIndex, int colIndex, int gridWidth) {
        return Point2D.newPoint2D(colIndex * gridWidth, rowIndex * gridWidth);
    }

    /**
     * 格子中心点坐标(针对宽高一样的格子)
     *
     * @param rowIndex  行索引，行索引对应的是Y索引 算出的是Y值
     * @param colIndex  列索引，列索引对应的是X索引 算出的是X值
     * @param gridWidth 格子宽度
     * @return centerPoint
     */
    public static Point2D gridCenterLocation(int rowIndex, int colIndex, int gridWidth) {
        return Point2D.newPoint2D((colIndex + 0.5f) * gridWidth, (rowIndex + 0.5f) * gridWidth);
    }
}
