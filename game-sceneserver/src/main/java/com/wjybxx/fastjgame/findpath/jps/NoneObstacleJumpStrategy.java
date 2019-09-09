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

import com.wjybxx.fastjgame.findpath.DiagonalMovement;
import com.wjybxx.fastjgame.scene.MapGrid;

import java.util.List;

import static com.wjybxx.fastjgame.findpath.FindPathUtils.addNeighborIfWalkable;

/**
 * 当前仅当前进的水平方向和垂直方向都不是遮挡时，才可以走对角线；
 * （不会因为对角线节点导致拐点，而只有水平和垂直方向会生成拐点，导致了拐点算法很不一样）
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/6/16 23:13
 * github - https://github.com/hl845740757
 */
public class NoneObstacleJumpStrategy extends JumpStrategy {

    @Override
    protected void findDiagonalNeighbors(JPSFindPathContext context, int x, int y, int dx, int dy, List<MapGrid> neighbors) {
        // 水平方向邻居
        boolean horizontalWalkable = addNeighborIfWalkable(context, x + dx, y, neighbors);
        // 垂直方向邻居
        boolean verticalWalkable = addNeighborIfWalkable(context, x, y + dy, neighbors);
        // 对角线邻居
        if (horizontalWalkable && verticalWalkable) {
            addNeighborIfWalkable(context, x + dx, y + dy, neighbors);
        }
    }

    /**
     * 为了保证对角线走过的区域能探测完，水平方向和垂直方向添加节点时需要特殊处理
     * （添加额外的节点）
     */
    @Override
    protected void findHorizontalNeighbors(JPSFindPathContext context, int x, int y, int dx, List<MapGrid> neighbors) {
        boolean isRightWalkable = addNeighborIfWalkable(context, x + dx, y, neighbors);
        boolean isUpperWalkable = addNeighborIfWalkable(context, x, y + 1, neighbors);
        boolean isLowerWalkable = addNeighborIfWalkable(context, x, y - 1, neighbors);

        if (isRightWalkable) {
            if (isUpperWalkable) {
                // 右上节点
                addNeighborIfWalkable(context, x + dx, y + 1, neighbors);
            }
            if (isLowerWalkable) {
                // 右下节点
                addNeighborIfWalkable(context, x + dx, y - 1, neighbors);
            }
        }
    }

    /**
     * 为了保证对角线走过的区域能探测完，水平方向和垂直方向添加节点时需要特殊处理
     * （添加额外的节点）
     */
    @Override
    protected void findVerticalNeighbors(JPSFindPathContext context, int x, int y, int dy, List<MapGrid> neighbors) {
        boolean isUpperWalkable = addNeighborIfWalkable(context, x, y + dy, neighbors);
        boolean isRightWalkable = addNeighborIfWalkable(context, x + 1, y, neighbors);
        boolean isLeftWalkable = addNeighborIfWalkable(context, x - 1, y, neighbors);

        if (isUpperWalkable) {
            if (isRightWalkable) {
                // 右上
                addNeighborIfWalkable(context, x + 1, y + dy, neighbors);
            }
            if (isLeftWalkable) {
                // 左上
                addNeighborIfWalkable(context, x - 1, y + dy, neighbors);
            }
        }
    }

    /**
     * 不会直接遇见拐点
     */
    @Override
    protected MapGrid diagonalJump(JPSFindPathContext context, int startX, int startY, int dx, int dy) {
        for (int currentX = startX, currentY = startY; ; currentX += dx, currentY += dy) {
            // 当前节点为目标节点
            // 如果点y是起点或目标点，则y是跳点
            if (context.isEndGrid(currentX, currentY)) {
                return context.endGrid;
            }

            // 水平或垂直方向遇见跳点，则将当前节点加入open集合（当前节点也是跳点）
            // 如果parent(y)到y是对角线移动，并且y经过水平或垂直方向移动可以到达跳点，则y是跳点
            if (tryHorizontalJump(context, currentX, currentY, dx) != null ||
                    tryVerticalJump(context, currentX, currentY, dy) != null) {
                return context.getGrid(currentX, currentY);
            }

            // 前进遇见遮挡
            if (!context.isWalkable(currentX + dx, currentY + dy)) {
                return null;
            }

            // 是否可以继续对角线移动
            if (!context.isWalkable(currentX + dx, currentY) || !context.isWalkable(currentX, currentY + dy)) {
                return null;
            }
        }
    }

    @Override
    protected MapGrid horizontalJump(JPSFindPathContext context, int startX, int currentY, int dx) {
        // 水平移动
        for (int currentX = startX; ; currentX += dx) {
            // 当前节点为目标节点
            if (context.isEndGrid(currentX, currentY)) {
                return context.endGrid;
            }

            // 是否是跳点（是否包含强迫邻居）
            if (hContainsTopForceNeighbor(context, currentX, currentY, dx) ||
                    hContainsLowerForceNeighbor(context, currentX, currentY, dx)) {
                return context.getGrid(currentX, currentY);
            }
            // 前进遇见遮挡
            if (!context.isWalkable(currentX + dx, currentY)) {
                return null;
            }
        }
    }


    /**
     * 是否存在上方强迫邻居
     *
     * <pre>
     *     |----|----|----|
     *     |    | X  |f(√)|
     *     |---------|----|
     *     |    | P  | C  |
     *     |---------|----|
     *     |    |    |    |
     *     |---------|----|
     *     P: parent
     *     C: current
     *     X: obstacle
     *     √: walkable
     *     f: forceNeighbor
     *     ?：unknown
     *     I: ignore
     * </pre>
     */
    private boolean hContainsTopForceNeighbor(JPSFindPathContext context, int x, int y, int dx) {
        return context.isWalkable(x, y + 1) && !context.isWalkable(x - dx, y + 1);
    }

    /**
     * 是否存在下方强迫邻居
     * <pre>
     *     |----|----|----|
     *     |    |    |    |
     *     |---------|----|
     *     |    | P  | C  |
     *     |---------|----|
     *     |    | X  |f(√)|
     *     |---------|----|
     *     P: parent
     *     C: current
     *     X: obstacle
     *     √: walkable
     *     f: forceNeighbor
     *     ?：unknown
     *     I: ignore
     * </pre>
     */
    private boolean hContainsLowerForceNeighbor(JPSFindPathContext context, int x, int y, int dx) {
        return context.isWalkable(x, y - 1) && !context.isWalkable(x - dx, y - 1);
    }

    @Override
    protected MapGrid verticalJump(JPSFindPathContext context, int currentX, int startY, int dy) {
        for (int currentY = startY; ; currentY += dy) {
            // 当前节点为目标节点
            if (context.isEndGrid(currentX, currentY)) {
                return context.endGrid;
            }
            // 不可对角线移动，那么只能左右拐，那么什么是时候拐方向？
            // （从parent(x)、x、n的路径长度比其他任何从parent(x)到n且不经过x的路径短，）（存在左右强迫邻居）
            if (vContainLeftForceNeighbor(context, currentX, currentY, dy)
                    || vContainRightForceNeighbor(context, currentX, currentY, dy)) {
                return context.getGrid(currentX, currentY);
            }

            // 前进遇见遮挡
            if (!context.isWalkable(currentX, currentY + dy)) {
                return null;
            }
        }
    }

    /**
     * （垂直移动时）当前节点是否存在左方强迫邻居;
     * <pre>
     *     |----|----|----|
     *     |f(√)| C  |    |
     *     |---------|----|
     *     | X  | P  |    |
     *     |---------|----|
     *     |    |    |    |
     *     |---------|----|
     *     P: parent
     *     C: current
     *     X: obstacle
     *     √: walkable
     *     f: forceNeighbor
     *     ?：unknown
     *     I: ignore
     * </pre>
     */
    private boolean vContainLeftForceNeighbor(JPSFindPathContext context, int x, int y, int dy) {
        return context.isWalkable(x - 1, y) && !context.isWalkable(x - 1, y - dy);
    }

    /**
     * （垂直移动时）当前节点是否存在右方强迫邻居;
     *
     * <pre>
     *     |----|----|----|
     *     |    | C  |f(√)|
     *     |---------|----|
     *     |    | P  | X  |
     *     |---------|----|
     *     |    |    |    |
     *     |---------|----|
     *     P: parent
     *     C: current
     *     X: obstacle
     *     √: walkable
     *     f: forceNeighbor
     *     ?：unknown
     *     I: ignore
     * </pre>
     */
    private boolean vContainRightForceNeighbor(JPSFindPathContext context, int x, int y, int dy) {
        return context.isWalkable(x + 1, y) && !context.isWalkable(x + 1, y - dy);

    }

    @Override
    protected DiagonalMovement diagonalMovement() {
        return DiagonalMovement.OnlyWhenNoObstacles;
    }
}
