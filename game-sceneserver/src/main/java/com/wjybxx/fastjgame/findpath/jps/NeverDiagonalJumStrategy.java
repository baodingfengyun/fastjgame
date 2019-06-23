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
import com.wjybxx.fastjgame.misc.Stateless;
import com.wjybxx.fastjgame.scene.MapGrid;

import java.util.List;

import static com.wjybxx.fastjgame.findpath.FindPathUtils.addNeighborIfWalkable;

/**
 * 禁止对角线移动
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/6/16 15:45
 * github - https://github.com/hl845740757
 */
@Stateless
public class NeverDiagonalJumStrategy extends JumpStrategy{

    @Override
    protected void findDiagonalNeighbors(JPSFindPathContext context, int x, int y, int dx, int dy, List<MapGrid> neighbors) {
        throw new IllegalStateException("unreachable!");
    }

    @Override
    protected void findHorizontalNeighbors(JPSFindPathContext context, int x, int y, int dx, List<MapGrid> neighbors) {
        // 水平移动， moving along x
        // 水平方向邻居
        addNeighborIfWalkable(context, x + dx, y, neighbors);
        // 上方邻居
        addNeighborIfWalkable(context, x, y + 1, neighbors);
        // 下方邻居
        addNeighborIfWalkable(context, x, y - 1, neighbors);
    }

    @Override
    protected void findVerticalNeighbors(JPSFindPathContext context, int x, int y, int dy, List<MapGrid> neighbors) {
        // 垂直移动, moving along y
        // 垂直方向邻居
        addNeighborIfWalkable(context, x, y + dy, neighbors);
        // 左方邻居
        addNeighborIfWalkable(context, x - 1, y, neighbors);
        // 右方邻居
        addNeighborIfWalkable(context, x + 1, y, neighbors);
    }

    @Override
    protected DiagonalMovement diagonalMovement() {
        return DiagonalMovement.Never;
    }

    @Override
    protected MapGrid diagonalJump(JPSFindPathContext context, int startX, int startY, int dx, int dy) {
        throw new IllegalStateException("unreachable");
    }

    @Override
    protected MapGrid horizontalJump(JPSFindPathContext context, int startX, int currentY, int dx) {
        // 水平移动
        for (int currentX=startX; ; currentX+=dx){
            // 当前节点为目标节点
            if (context.isEndGrid(currentX, currentY)){
                return context.endGrid;
            }
            // 不可以对角线移动，那么只能上下拐，什么时候拐？
            // （从parent(x)、x、n的路径长度比其他任何从parent(x)到n且不经过x的路径短）（存在上下强迫邻居）
            // 存在上下强制邻居
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
     * (水平移动时，向右)是否存在上方强迫邻居
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
     * (水平移动时，向右)是否存在下方强迫邻居
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
        for (int currentY = startY; ; currentY += dy){
            // 当前节点为目标节点
            if (context.isEndGrid(currentX, currentY)){
                return context.endGrid;
            }
            // 不可对角线移动，那么只能左右拐，那么什么是时候拐方向？
            // （从parent(x)、x、n的路径长度比其他任何从parent(x)到n且不经过x的路径短，）（存在左右强迫邻居）
            if (vContainLeftForceNeighbor(context, currentX, currentY, dy)
                    || vContainRightForceNeighbor(context, currentX, currentY, dy)){
                return context.getGrid(currentX, currentY);
            }

            // 不可以对角线移动的时候，水平和垂直方向，必须有一个方向要双向跳跃(否则无法处理到探索区域的所有地图格子)
            // When moving vertically, must check for horizontal jump points
            // 左右跳跃(探索)
            if (tryHorizontalJump(context, currentX, currentY, -1) != null ||
                    tryHorizontalJump(context, currentX, currentY, 1) != null) {
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
    private boolean vContainLeftForceNeighbor(JPSFindPathContext context, int x, int y, int dy){
        return context.isWalkable(x - 1, y) && !context.isWalkable(x - 1, y - dy);
    }

    /**
     * （垂直移动时）当前节点是否存在右方强迫邻居;
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
    private boolean vContainRightForceNeighbor(JPSFindPathContext context, int x, int y, int dy){
        return context.isWalkable(x + 1, y) && !context.isWalkable(x + 1, y - dy);
    }
}
