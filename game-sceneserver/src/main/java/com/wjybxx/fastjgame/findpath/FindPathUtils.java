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
import com.wjybxx.fastjgame.shape.Point2D;

import java.util.Arrays;
import java.util.List;

/**
 * 寻路过程中的一些辅助方法
 * @author wjybxx
 * @version 1.0
 * @date 2019/6/12 15:13
 * @github - https://github.com/hl845740757
 */
public class FindPathUtils {

    /**
     * 邻居节点偏移量
     */
    public static final NeighborOffSet[] neighborOffsetArray = {
            new NeighborOffSet(-1,  1), new NeighborOffSet(0,  1), new NeighborOffSet(1,  1),
            new NeighborOffSet(-1,  0),                        new NeighborOffSet(1,  0),
            new NeighborOffSet(-1, -1), new NeighborOffSet(0, -1), new NeighborOffSet(1, -1)
    };

    private FindPathUtils() {

    }

    /**
     * 如果指定格子可行走，则添加到neighbor中
     * @param context
     * @param nx neighborX
     * @param ny neighborY
     * @param neighbors
     * @return true/false 添加成功则返回true，失败返回false
     */
    public static boolean addNeighborIfWalkable(FindPathContext context, int nx, int ny, List<MapGrid> neighbors){
        if (context.isWalkable(nx, ny)){
            neighbors.add(context.getGrid(nx, ny));
            return true;
        }else {
            return false;
        }
    }

    /**
     * 获取当前节点的所有邻居节点
     * @param context 寻路上下文
     * @param curNode 当前节点
     * @param diagonalMovement 对角线行走策略
     * @param neighbors 邻居节点容器，传入以方便子类使用缓存
     */
    public static void statisticsNeighbors(FindPathContext context,
                                           FindPathNode curNode,
                                           DiagonalMovement diagonalMovement,
                                           List<MapGrid> neighbors) {
        boolean up    = false, leftUpper  = false,
                right = false, rightUpper = false,
                down  = false, rightLower = false,
                left  = false, leftLower  = false;

        final int x = curNode.getX();
        final int y = curNode.getY();

        // 上
        if (context.isWalkable(x, y + 1)) {
            neighbors.add(context.getGrid(x, y + 1));
            up = true;
        }

        // 右
        if (context.isWalkable(x + 1, y)) {
            neighbors.add(context.getGrid(x + 1, y));
            right = true;
        }

        // 下
        if (context.isWalkable(x, y - 1)) {
            neighbors.add(context.getGrid(x, y - 1));
            down = true;
        }

        // 左
        if (context.isWalkable(x - 1, y)) {
            neighbors.add(context.getGrid(x - 1, y));
            left = true;
        }

        switch (diagonalMovement)
        {
            case Never:
                // 不必执行后面部分
                return;

            case Always:
                leftUpper = true;
                rightUpper = true;
                rightLower = true;
                leftLower = true;
                break;

            case AtLeastOneWalkable:
                leftUpper = left || up;
                rightUpper = right || up;
                rightLower = right || down;
                leftLower = left || down ;
                break;

            case OnlyWhenNoObstacles:
                leftUpper = left && up;
                rightUpper = right && up;
                rightLower = right && down;
                leftLower = left && down ;
                break;

            default:
                break;
        }

        // 左上
        if (leftUpper && context.isWalkable(x - 1, y + 1)) {
            neighbors.add(context.getGrid(x - 1, y + 1));
        }

        // 右上
        if (rightUpper && context.isWalkable(x + 1, y + 1)) {
            neighbors.add(context.getGrid(x + 1, y + 1));
        }

        // 右下
        if (rightLower && context.isWalkable(x + 1, y - 1)) {
            neighbors.add(context.getGrid(x + 1, y - 1));
        }

        // 左下
        if (leftLower && context.isWalkable(x - 1, y - 1)) {
            neighbors.add(context.getGrid(x - 1, y - 1));
        }
    }

    /**
     * 构建最终路径
     * @param mapData 地图数据
     * @param finalPathNode
     * @return
     */
    public static List<Point2D> buildFinalPath(MapData mapData, final FindPathNode finalPathNode){
        Point2D[] pathArray = new Point2D[finalPathNode.getDepth() + 1];
        for (FindPathNode tempNode = finalPathNode; tempNode!= null; tempNode=tempNode.getParent()){
            MapGrid mapGrid = mapData.getGrid(tempNode.getX(), tempNode.getY());
            pathArray[tempNode.getDepth()] = Point2D.newPoint2D(mapGrid.getCenter());
        }
        return Arrays.asList(pathArray);
    }

}
