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

import com.wjybxx.fastjgame.scene.MapGrid;
import com.wjybxx.fastjgame.shape.Point2D;
import com.wjybxx.fastjgame.utils.MathUtils;

import java.util.Arrays;
import java.util.List;

/**
 * 寻路过程中的一些辅助方法
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/6/12 15:13
 * github - https://github.com/hl845740757
 */
public class FindPathUtils {

    /**
     * 邻居节点偏移量
     */
    public static final NeighborOffSet[] neighborOffsetArray = {
            new NeighborOffSet(-1, 1), new NeighborOffSet(0, 1), new NeighborOffSet(1, 1),
            new NeighborOffSet(-1, 0), new NeighborOffSet(1, 0),
            new NeighborOffSet(-1, -1), new NeighborOffSet(0, -1), new NeighborOffSet(1, -1)
    };

    private FindPathUtils() {

    }

    /**
     * 如果指定格子可行走，则添加到neighbor中
     *
     * @param context   寻路上下文
     * @param nx        neighborX
     * @param ny        neighborY
     * @param neighbors 邻居节点容器，传入以方便使用缓存
     * @return true/false 添加成功则返回true，失败返回false
     */
    public static boolean addNeighborIfWalkable(FindPathContext context, int nx, int ny, List<MapGrid> neighbors) {
        if (context.isWalkable(nx, ny)) {
            neighbors.add(context.getGrid(nx, ny));
            return true;
        } else {
            return false;
        }
    }

    /**
     * 获取当前节点的所有邻居节点
     *
     * @param context          寻路上下文
     * @param x                当前x坐标
     * @param y                当前y坐标
     * @param diagonalMovement 对角线行走策略
     * @param neighbors        邻居节点容器，传入以方便使用缓存
     */
    public static void statisticsNeighbors(FindPathContext context,
                                           final int x, final int y,
                                           DiagonalMovement diagonalMovement,
                                           List<MapGrid> neighbors) {
        boolean up = false, leftUpper = false,
                right = false, rightUpper = false,
                down = false, rightLower = false,
                left = false, leftLower = false;

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

        switch (diagonalMovement) {
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
                leftLower = left || down;
                break;

            case OnlyWhenNoObstacles:
                leftUpper = left && up;
                rightUpper = right && up;
                rightLower = right && down;
                leftLower = left && down;
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
     *
     * @param context       寻路信息
     * @param finalPathNode 寻到的最终节点
     * @return 去除了冗余节点的路径
     */
    public static List<Point2D> buildFinalPath(FindPathContext context, final FindPathNode finalPathNode) {
        Point2D[] pathArray = new Point2D[finalPathNode.getDepth() + 1];
        for (FindPathNode tempNode = finalPathNode; tempNode != null; tempNode = tempNode.getParent()) {
            MapGrid mapGrid = context.getGrid(tempNode.getX(), tempNode.getY());
            pathArray[tempNode.getDepth()] = Point2D.newPoint2D(mapGrid.getCenter());
        }
        return Arrays.asList(pathArray);
    }

    /**
     * 路径平滑处理（暂时只是去掉冗余点，还未真正做到完全平滑）
     * - https://blog.csdn.net/lqk1985/article/details/6679754
     */
    public static List<Point2D> buildSmoothPath(FindPathContext context, final FindPathNode finalPathNode) {
        deleteRedundantNode(context, finalPathNode);
        return buildFinalPath(context, finalPathNode);
    }

    /**
     * 删除路径中的冗余节点（不存在三点共线）
     */
    private static void deleteRedundantNode(FindPathContext context, FindPathNode finalPathNode) {
        FindPathNode startNode = null;
        FindPathNode midNode = null;

        Point2D a;
        Point2D b;
        Point2D c;

        for (FindPathNode tempNode = finalPathNode; tempNode != null; tempNode = tempNode.getParent()) {
            if (null == startNode) {
                startNode = tempNode;
                continue;
            }

            if (null == midNode) {
                midNode = tempNode;
                continue;
            }

            a = context.getGrid(startNode.getX(), startNode.getY()).getCenter();
            b = context.getGrid(midNode.getX(), midNode.getY()).getCenter();
            c = context.getGrid(tempNode.getX(), tempNode.getY()).getCenter();

            if (MathUtils.isOneLine(a, b, c) || isNoObstacle(context, a, c)) {
                // 如果三点共线，或者a可以跳过b直达c
                // delete mid node
                startNode.setParent(tempNode);
                midNode = tempNode;
            } else {
                // 从tempNode重新开始
                startNode = tempNode;
                midNode = null;
            }
        }
    }

    /**
     * 两点之间是否没有遮挡(可直达)
     *
     * @param context
     * @return
     */
    public static boolean isNoObstacle(FindPathContext context, Point2D a, Point2D b) {
        // TODO
        return false;
    }

}
