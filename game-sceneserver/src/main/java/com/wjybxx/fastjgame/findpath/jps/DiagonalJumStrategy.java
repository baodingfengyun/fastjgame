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

import com.wjybxx.fastjgame.scene.MapGrid;

import java.util.List;

import static com.wjybxx.fastjgame.findpath.FindPathUtils.addNeighborIfWalkable;

/**
 * 遇见拐点可对角线拐的策略，可对角线进行跳跃的跳跃的策略。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/6/16 21:52
 * github - https://github.com/hl845740757
 */
public abstract class DiagonalJumStrategy extends JumpStrategy {

    // --------------------------------查找邻居------------------------------
    @Override
    protected void findDiagonalNeighbors(JPSFindPathContext context, int x, int y, int dx, int dy, List<MapGrid> neighbors) {
        // 水平方向邻居
        boolean horizontalWalkable = addNeighborIfWalkable(context, x + dx, y, neighbors);
        // 垂直方向邻居
        boolean verticalWalkable = addNeighborIfWalkable(context, x, y + dy, neighbors);
        // 对角线邻居
        if (allowMovingAlongDiagonal(horizontalWalkable, verticalWalkable)) {
            addNeighborIfWalkable(context, x + dx, y + dy, neighbors);
        }
        // 左上强迫邻居
        if (dContainsLeftUpperForceNeighbor(context, x, y, dx, dy)) {
            neighbors.add(context.getGrid(x - dx, y + dy));
        }
        // 右下强迫邻居
        if (dContainsRightLowerForceNeighbor(context, x, y, dx, dy)) {
            neighbors.add(context.getGrid(x + dx, y - dy));
        }
    }

    /**
     * 对角线非遮挡时，是否允许对角线移动
     *
     * @param horizontalWalkable 水平方向是否可行走
     * @param verticalWalkable   垂直方向是否可行走
     * @return true/false
     */
    protected abstract boolean allowMovingAlongDiagonal(boolean horizontalWalkable, boolean verticalWalkable);

    /**
     * （对角线移动，右上移动时）是否包含左上方强迫邻居
     * <pre>
     *     |----|----|----|
     *     |f(√)| ?  |    |
     *     |---------|----|
     *     | X  | C  |    |
     *     |---------|----|
     *     | P  |    |    |
     *     |---------|----|
     *     P: parent
     *     C: current
     *     X: obstacle
     *     √: walkable
     *     f: forceNeighbor
     *     ?：unknown
     *     I: ignore
     * </pre>
     *
     * @param context 寻路上下文
     * @param x       当前格子x坐标
     * @param y       当前格子y坐标
     * @param dx      deltaX x增量
     * @param dy      deltaY y增量
     * @return
     */
    private boolean dContainsLeftUpperForceNeighbor(JPSFindPathContext context, int x, int y, int dx, int dy) {
        return !context.isWalkable(x - dx, y) &&
                context.isWalkable(x - dx, y + dy) &&
                allowMovingAlongDiagonal(false, context.isWalkable(x, y + dy));
    }

    /**
     * （对角线移动，右上移动时）是否包含右下方强迫邻居
     * <pre>
     *     |----|----|----|
     *     |    |    |    |
     *     |---------|----|
     *     |    | C  | ?  |
     *     |---------|----|
     *     | P  | X  |f(√)|
     *     |---------|----|
     *     P: parent
     *     C: current
     *     X: obstacle
     *     √: walkable
     *     f: forceNeighbor
     *     ?：unknown
     *     I: ignore
     * </pre>
     *
     * @param context 寻路上下文
     * @param x       当前格子x坐标
     * @param y       当前格子y坐标
     * @param dx      deltaX x增量
     * @param dy      deltaY y增量
     */
    private boolean dContainsRightLowerForceNeighbor(JPSFindPathContext context, int x, int y, int dx, int dy) {
        // 右下强迫邻居
        return !context.isWalkable(x, y - dy) &&
                context.isWalkable(x + dx, y - dy) &&
                // 用查询代替临时变量，可是老是纠结多算一遍
                allowMovingAlongDiagonal(context.isWalkable(x + dx, y), false);
    }

    @Override
    protected void findHorizontalNeighbors(JPSFindPathContext context, int x, int y, int dx, List<MapGrid> neighbors) {
        // 水平方向邻居
        addNeighborIfWalkable(context, x + dx, y, neighbors);
        // 右上强迫邻居
        if (hContainsRightUpperForceNeighbor(context, x, y, dx)) {
            neighbors.add(context.getGrid(x + dx, y + 1));
        }
        // 右下强迫邻居
        if (hContainsRightLowerForceNeighbor(context, x, y, dx)) {
            neighbors.add(context.getGrid(x + dx, y - 1));
        }
    }

    /**
     * （水平方向移动,向右移动）是否包含右上强迫邻居
     * <pre>
     *     |----|----|----|
     *     |    | X  |f(√)|
     *     |---------|----|
     *     |  P | C  | ?  |
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
     *
     * @param context 寻路上下文
     * @param x       当前格子x坐标
     * @param y       当前格子y坐标
     * @param dx      deltaX x增量
     */
    private boolean hContainsRightUpperForceNeighbor(JPSFindPathContext context, int x, int y, int dx) {
        // 上方不可行走，但是斜上方可行走，如果可走对角线的话，则当前节点是跳点(拐点)
        return !context.isWalkable(x, y + 1) &&
                context.isWalkable(x + dx, y + 1) &&
                allowMovingAlongDiagonal(context.isWalkable(x + dx, y), false);
    }

    /**
     * （水平方向移动，向右移动）是否包含右下强迫邻居
     * <pre>
     *     |----|----|----|
     *     |    |    |    |
     *     |---------|----|
     *     |  P | C  | ?  |
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
     *
     * @param context 寻路上下文
     * @param x       当前格子x坐标
     * @param y       当前格子y坐标
     * @param dx      deltaX x增量
     */
    private boolean hContainsRightLowerForceNeighbor(JPSFindPathContext context, int x, int y, int dx) {
        // 下方不可行走，但是斜下方可行走，如果对角线可行走的话，则当前节点是跳点(拐点)
        return !context.isWalkable(x, y - 1) &&
                context.isWalkable(x + dx, y - 1) &&
                allowMovingAlongDiagonal(context.isWalkable(x + dx, y), false);
    }

    @Override
    protected void findVerticalNeighbors(JPSFindPathContext context, int x, int y, int dy, List<MapGrid> neighbors) {
        // 垂直方向邻居
        addNeighborIfWalkable(context, x, y + dy, neighbors);
        // 左上强迫邻居
        if (vContainsLeftUpperForceNeighbor(context, x, y, dy)) {
            neighbors.add(context.getGrid(x - 1, y + dy));
        }
        // 右上强迫邻居
        if (vContainsRightUpperForceNeighbor(context, x, y, dy)) {
            neighbors.add(context.getGrid(x + 1, y + dy));
        }
    }

    /**
     * （垂直方向移动，向上移动时）是否包含左上方强迫邻居
     * <pre>
     *     |----|----|----|
     *     |f(√)| ?  |    |
     *     |---------|----|
     *     | X  | C  |    |
     *     |---------|----|
     *     |    | P  |    |
     *     |---------|----|
     *     P: parent
     *     C: current
     *     X: obstacle
     *     √: walkable
     *     f: forceNeighbor
     *     ?：unknown
     *     I: ignore
     * </pre>
     *
     * @param context 寻路上下文
     * @param x       当前格子x坐标
     * @param y       当前格子y坐标
     * @param dy      deltaY y增量
     */
    private boolean vContainsLeftUpperForceNeighbor(JPSFindPathContext context, int x, int y, int dy) {
        // 左方不可行走，但是左下方可行走，如果该对角线方向可行走的话，则当前节点是跳点(拐点)
        return !context.isWalkable(x - 1, y) &&
                context.isWalkable(x - 1, y + dy) &&
                allowMovingAlongDiagonal(false, context.isWalkable(x, y + dy));
    }

    /**
     * （垂直方向移动，向上移动时）是否包含右上方强迫邻居
     *
     * <pre>
     *     |----|----|----|
     *     |    | ?  |f(√)|
     *     |---------|----|
     *     |    | C  | X  |
     *     |---------|----|
     *     |    | P  |    |
     *     |---------|----|
     *     P: parent
     *     C: current
     *     X: obstacle
     *     √: walkable
     *     f: forceNeighbor
     *     ?：unknown
     *     I: ignore
     * </pre>
     *
     * @param context 寻路上下文
     * @param x       当前格子x坐标
     * @param y       当前格子y坐标
     * @param dy      deltaY y增量
     */
    private boolean vContainsRightUpperForceNeighbor(JPSFindPathContext context, int x, int y, int dy) {
        // 右方不可行走，但是右上方可行走，如果该对角线方向可行走的话，则当前节点是跳点(拐点)
        return !context.isWalkable(x + 1, y) &&
                context.isWalkable(x + 1, y + dy) &&
                allowMovingAlongDiagonal(false, context.isWalkable(x, y + dy));
    }

    // --------------------------------跳跃---------------------------------

    @Override
    protected MapGrid diagonalJump(JPSFindPathContext context, int startX, int startY, int dx, int dy) {
        for (int currentX = startX, currentY = startY; ; currentX += dx, currentY += dy) {
            // 当前节点为目标节点
            // 如果点y是起点或目标点，则y是跳点
            if (context.isEndGrid(currentX, currentY)) {
                return context.endGrid;
            }
            // 如果点n是x的邻居，并且点n的邻居有阻挡（不可行走的格子），
            // 并且从parent(x)、x、n的路径长度比其他任何从parent(x)到n且不经过x的路径短，
            // 其中parent(x)为路径中x的前一个点，则n为x的强迫邻居，x为n的跳点）

            if (dContainsLeftUpperForceNeighbor(context, currentX, currentY, dx, dy) ||
                    dContainsRightLowerForceNeighbor(context, currentX, currentY, dx, dy)) {
                return context.getGrid(currentX, currentY);
            }

            // 水平或垂直方向遇见跳点，则将当前节点加入open集合（当前节点也是跳点）
            // 如果parent(y)到y是对角线移动，并且y经过水平或垂直方向移动可以到达跳点，则y是跳点
            if (tryHorizontalJump(context, currentX, currentY, dx) != null ||
                    tryVerticalJump(context, currentX, currentY, dy) != null) {
                return context.getGrid(currentX, currentY);
            }

            // 对角线格子是遮挡
            if (!context.isWalkable(currentX + dx, currentY + dy)) {
                return null;
            }

            // 是否可以继续对角线移动
            if (!allowMovingAlongDiagonal(context.isWalkable(currentX + dx, currentY),
                    context.isWalkable(currentX, currentY + dy))) {
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
            if (hContainsRightUpperForceNeighbor(context, currentX, currentY, dx) ||
                    hContainsRightLowerForceNeighbor(context, currentX, currentY, dx)) {
                return context.getGrid(currentX, currentY);
            }
            // 前进遇见遮挡
            if (!context.isWalkable(currentX + dx, currentY)) {
                return null;
            }
        }
    }

    @Override
    protected MapGrid verticalJump(JPSFindPathContext context, int currentX, int startY, int dy) {
        for (int currentY = startY; ; currentY += dy) {
            // 当前节点为目标节点
            if (context.isEndGrid(currentX, currentY)) {
                return context.endGrid;
            }
            // 是否是跳点（是否包含强迫邻居）
            if (vContainsLeftUpperForceNeighbor(context, currentX, currentY, dy) ||
                    vContainsRightUpperForceNeighbor(context, currentX, currentY, dy)) {
                return context.getGrid(currentX, currentY);
            }
            // 前进遇见遮挡
            if (!context.isWalkable(currentX, currentY + dy)) {
                return null;
            }
        }
    }
}
