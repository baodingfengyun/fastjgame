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
import com.wjybxx.fastjgame.findpath.FindPathNode;
import com.wjybxx.fastjgame.findpath.FindPathUtils;
import com.wjybxx.fastjgame.scene.MapGrid;

import java.util.List;

import static com.wjybxx.fastjgame.findpath.FindPathUtils.addNeighborIfWalkable;

/**
 * 当存在非遮挡格子可达对角线时，可走对角线
 *
 * 参考自：
 *  - https://github.com/qiao/PathFinding.js/blob/master/src/finders/JPFMoveDiagonallyIfAtMostOneObstacle.js
 *
 * @author wjybxx
 * @version 1.0
 * @date 2019/6/12 15:54
 * @github - https://github.com/hl845740757
 */
public class AtLeastOneWalkableJumpStrategy extends JumpStrategy {

    @Override
    public void findNeighbors(JPSFindPathContext context, FindPathNode curNode, List<MapGrid> neighbors) {
        FindPathNode parent = curNode.getParent();
        if (parent != null) {
            // return special direction neighbors 返回特定方向的邻居节点
            final int x = curNode.getX();
            final int y = curNode.getY();

            // get normalized direction of travel
            // 归一化为水平和垂直方向的单位向量(-1,0,1)，用于判断当前节点的探测方向，我居然想到了compare这个骚操作
            final int dx = Integer.compare(x, parent.getX());
            final int dy = Integer.compare(y, parent.getY());

            // 对角线的时候，正常需要探测三个邻居， 水平方向一个，垂直方向一个，对角线方向一个
            if (dx !=0 && dy != 0){
                // 寻找对角线方向邻居

                // 垂直方向邻居
                boolean vertically = addNeighborIfWalkable(context, x, y + dy, neighbors);
                // 水平方向邻居
                boolean horizontally = addNeighborIfWalkable(context, x + dx, y, neighbors);

                // 对角线邻居（对角线方向，水平方向和垂直方向至少有一个节点可行走时，才可以走对角线）
                if (vertically || horizontally) {
                    addNeighborIfWalkable(context, x + dx, y + dy, neighbors);
                }

                // 检查垂直方向强制邻居
                if (!context.isWalkable(x - dx, y) && vertically) {
                    addNeighborIfWalkable(context, x - dx, y + dy, neighbors);
                }

                // 检查水平方向强制邻居
                if (!context.isWalkable(x, y - dy) && horizontally) {
                    addNeighborIfWalkable(context, x + dx, y - dy, neighbors);
                }
            }else {
                if (dx != 0){
                    // dx != 0 表示水平方向，寻找水平方向邻居
                    if (addNeighborIfWalkable(context, x + dx, y, neighbors)) {
                        // 走对角线必须有一个邻居可行走，水平方向可行走时，可以走对角线，检查强制邻居
                        if (!context.isWalkable(x, y + 1)) {
                            addNeighborIfWalkable(context, x + dx, y + 1, neighbors);
                        }

                        if (!context.isWalkable(x, y - 1)) {
                            addNeighborIfWalkable(context, x + dx, y - 1, neighbors);
                        }
                    }
                }else {
                    // dy != 0 表示垂直方向，寻找垂直方向邻居
                    if (addNeighborIfWalkable(context, x, y + dy, neighbors)) {
                        // 走对角线必须有一个邻居可行走，垂直方向可行走时，可以走对角线，检查强制邻居
                        if (!context.isWalkable(x + 1, y)) {
                            addNeighborIfWalkable(context, x + 1, y + dy, neighbors);
                        }

                        if (!context.isWalkable(x - 1, y)) {
                            addNeighborIfWalkable(context, x - 1, y + dy, neighbors);
                        }
                    }
                }
            }
        }else {
            // no parent, return all neighbors 返回所有可行走的邻居
            FindPathUtils.statisticsNeighbors(context, curNode, DiagonalMovement.AtLeastOneWalkable, neighbors);
        }
    }

    /**
     * 对角线移动
     */
    protected MapGrid diagonalJump(JPSFindPathContext context, int startX, int startY, int dx, int dy, int endX, int endY) {
        for (int currentX = startX, currentY = startY; ; currentX+=dx, currentY+=dy){
            // 当前点是遮挡点
            if (!context.isWalkable(currentX, currentY)) {
                return null;
            }
            // 当前节点为目标节点
            if (currentX == endX && currentY == endY){
                return context.getGrid(currentX, currentY);
            }

            if (!context.isWalkable(currentX - dx, currentY) && context.isWalkable(currentX - dx, currentY + dy) ||
                    !context.isWalkable(currentX, currentY - dy) && context.isWalkable(currentX + dx, currentY - dy)) {
                return context.getGrid(currentX, currentY);
            }

            // 水平或垂直方向遇见跳点，则将当前节点加入open集合（当前节点也是跳点）
            if (horizontalJump(context, currentX, currentY, dx, endX, endY) != null ||
                    verticalJump(context, currentX, currentY, dy, endX, endY) != null){
                return context.getGrid(currentX, currentY);
            }

            // 对角线移动，必须保证水平方向和垂直方向有一方可走，邻居是开放的，允许通行
            // moving diagonally, must make sure one of the vertical/horizontal
            // neighbors is open to allow the path
            if (context.isWalkable(currentX, currentY + dy) || context.isWalkable(currentX + dx, currentY)){
                continue;
            }else {
                return null;
            }
        }
    }

    /**
     * 水平跳跃
     */
    @Override
    protected MapGrid horizontalJump(JPSFindPathContext context, final int startX, final int currentY, int dx, int endX, int endY){
        // 水平移动
        for (int currentX=startX; ; currentX+=dx){
            // 当前点是遮挡点（或越界）
            if (!context.isWalkable(currentX, currentY)) {
                return null;
            }
            // 当前节点为目标节点
            if (currentX == endX && currentY == endY){
                return context.getGrid(currentX, currentY);
            }
            // 上方不可行走，但是斜上方可行走，则当前节点是跳点(拐点)
            // 下方不可行走，但是斜下方可行走，则当前节点是跳点(拐点)
            if ((!context.isWalkable(currentX, currentY + 1) && (context.isWalkable(currentX + dx, currentY + 1))) ||
                    (!context.isWalkable(currentX, currentY - 1) && context.isWalkable(currentX + dx, currentY - 1))) {
                return context.getGrid(currentX, currentY);
            }
        }
    }

    /**
     * 垂直跳跃
     */
    @Override
    protected MapGrid verticalJump(JPSFindPathContext context,final int currentX,final int startY, int dy, int endX, int endY){
        for (int currentY = startY; ; currentY += dy){
            // 当前点是遮挡点
            if (!context.isWalkable(currentX, currentY)) {
                return null;
            }
            // 当前节点为目标节点
            if (currentX == endX && currentY == endY){
                return context.getGrid(currentX, currentY);
            }

            // 右方不可行走，但是右上方可行走，则当前节点是跳点(拐点)
            // 左方不可行走，但是左下方可行走，则当前节点是跳点(拐点)
            if ((!context.isWalkable(currentX + 1, currentY) && context.isWalkable(currentX + 1, currentY + dy)) ||
                    (!context.isWalkable(currentX - 1, currentY) && context.isWalkable(currentX - 1, currentY + dy))) {
                return context.getGrid(currentX, currentY);
            }
        }
    }
}
