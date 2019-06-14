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

import javax.annotation.Nullable;
import java.util.List;

/**
 * 当存在非遮挡格子可达对角线时，可走对角线
 *
 * 参考自：
 *  - https://github.com/kevinsheehan/jps
 *
 * @author wjybxx
 * @version 1.0
 * @date 2019/6/12 15:54
 * @github - https://github.com/hl845740757
 */
public class AtLeastOneWalkableStrategy implements DiagonalMoveStrategy{

    @Override
    public void findNeighbors(JPSFindPathContext context, FindPathNode curNode, List<MapGrid> neighbors) {
        FindPathNode parent = curNode.getParent();
        if (parent != null) {
            final int x = curNode.getX();
            final int y = curNode.getY();

            // get normalized direction of travel
            // 归一化为水平和垂直方向的单位向量(-1,0,1)，用于判断当前节点的探测方向，我居然想到了compare这个骚操作
//            final int dx = Integer.compare(x, parent.getX());
//            final int dy = Integer.compare(y, parent.getY());
            final int dx = (x - parent.getX()) / Math.max(Math.abs(x - parent.getX()), 1);
            final int dy = (y - parent.getY()) / Math.max(Math.abs(y - parent.getY()), 1);

            // search diagonally
            // dx 和 dy 都不为0，表示对角线方向
            if (dx != 0 && dy != 0) {
                boolean vertically = false;
                boolean horizontally = false;

                // 对角线的时候，需要探测三个邻居， 水平方向一个，垂直方向一个，对角线方向一个
                // 垂直方向邻居
                if (context.isWalkable(x, y + dy)) {
                    neighbors.add(context.getGrid(x, y + dy));
                    vertically = true;
                }

                // 水平方向邻居
                if (context.isWalkable(x + dx, y)) {
                    neighbors.add(context.getGrid(x + dx, y));
                    horizontally = true;
                }

                // 对角线邻居（对角线方向，水平方向和垂直方向至少有一个节点可行走时，才可以走对角线）
                if ((vertically || horizontally) && context.isWalkable(x + dx, y + dy)) {
                    neighbors.add(context.getGrid(x + dx, y + dy));
                }

                // 检查强制邻居（水平方向的逆方向是否遮挡，如果有遮挡，
                // 那么对于该遮挡的格子的邻居，表示无法找到一条通过parent而不通过curNode到它们的更近的道路）
                // （强制邻居也是当前节点对角线，需要满足水平垂直方向至少有一个可走）
                if ((!context.isWalkable(x - dx, y) && vertically) && context.isWalkable(x - dx, y + dy)) {
                    neighbors.add(context.getGrid(x - dx, y + dy));
                }

                // 检查强制邻居（垂直方向的逆方向是否遮挡）
                // （强制邻居也是当前节点对角线，需要满足水平垂直方向至少有一个可走）
                if ((!context.isWalkable(x, y - dy) && horizontally) && context.isWalkable(x + dx, y - dy)) {
                    neighbors.add(context.getGrid(x + dx, y - dy));
                }
            } else {
                // search horizontally/vertically
                if (dx == 0) {
                    // dx 为 0 表示垂直方向
                    if (context.isWalkable(x, y + dy)) {
                        // 添加垂直方向邻居
                        neighbors.add(context.getGrid(x, y + dy));

                        // 检查强制邻居（左右是否有遮挡，如果左右有遮挡，
                        // 那么对于该遮挡的格子的邻居，表示无法找到一条通过parent而不通过curNode到它们的更近的道路）
                        if (!context.isWalkable(x + 1, y) && context.isWalkable(x + 1, y + dy)) {
                            neighbors.add(context.getGrid(x + 1, y + dy));
                        }

                        // 检查强制邻居
                        if (!context.isWalkable(x - 1, y) && context.isWalkable(x - 1, y + dy)) {
                            neighbors.add(context.getGrid(x - 1, y + dy));
                        }
                    }
                } else {
                    // dy 为 0 表示水平方向
                    if (context.isWalkable(x + dx, y)) {
                        // 检查水平方向邻居
                        neighbors.add(context.getGrid(x + dx, y));

                        // 检查强制邻居（上下是否有遮挡，如果上下有遮挡，
                        // 那么对于该遮挡的格子的邻居，表示无法找到一条通过parent而不通过curNode到它们的更近的道路）
                        if (!context.isWalkable(x, y + 1) && context.isWalkable(x + dx, y + 1)) {
                            neighbors.add(context.getGrid(x + dx, y + 1));
                        }

                        // 检查强制邻居
                        if (!context.isWalkable(x, y - 1) && context.isWalkable(x + dx, y - 1)) {
                            neighbors.add(context.getGrid(x + dx, y - 1));
                        }
                    }
                }
            }
        } else {
            // no parent, return all neighbors
            FindPathUtils.statisticsNeighbors(context.mapData, curNode,
                    DiagonalMovement.AtLeastOneWalkable, context.walkableGridStrategy,neighbors);
        }
    }

    @Nullable
    @Override
    public MapGrid jump(JPSFindPathContext context, int parentX, int parentY, int currentX, int currentY) {
        if (!context.isWalkable(currentX, currentY)){
            return null;
        }

        // 当前节点目标节点
        if (context.endGrid.getX() == currentX && context.endGrid.getY() == currentY){
            return context.getGrid(currentX, currentY);
        }

        // 当前节点与父节点的x和y增量区间为[-1,0,1]
        int dx = currentX - parentX;
        int dy = currentY - parentY;

        // check for forced neighbors
        // check along diagonal
        if (dx != 0 && dy != 0) {
            // dx 与 dy都不为0， 表示对角线方向跳跃
            // 当前方向的水平方向和垂直方向有遮挡，存在强制邻居，该节点是跳点
            if ((context.isWalkable(currentX - dx, currentY + dy) && !context.isWalkable(currentX - dx, currentY)) ||
                    (context.isWalkable(currentX + dx, currentY - dy) && !context.isWalkable(currentX, currentY - dy))) {
                return context.getGrid(currentX, currentY);
            }

            // 进行对角线方向跳跃的时候，需要先进行水平方向和垂直方向跳跃
            // 水平和垂直方向跳跃，如果遇见跳点，则当前节点是跳点（拐点）
            // when moving diagonally, must check for vertical/horizontal jump points

            // 水平/垂直方向发现跳点(对角线跳跃时，水平垂直方向发现跳点，返回当前点)，将当前节点加入openSet
            if (jump(context, currentX, currentY, currentX + dx, currentY) != null ||
                    jump(context, currentX, currentY, currentX, currentY + dy) != null){
                return context.getGrid(currentX, currentY);
            }
        } else {
            // check horizontally/vertically
            if (dx != 0) {
                // dx 不为0，表示 水平方向跳跃
                // 斜上方可行走，但是上方不可行走，则当前节点是跳点(拐点)
                // 斜下方可行走，但是下方不可行走，则当前节点是跳点(拐点)
                if ((context.isWalkable(currentX + dx, currentY + 1) && !context.isWalkable(currentX, currentY + 1)) ||
                        (context.isWalkable(currentX + dx, currentY - 1) && !context.isWalkable(currentX, currentY - 1))) {
                    return context.getGrid(currentX, currentY);
                }
            } else {
                // dy 不为0，表示 垂直方向跳跃
                // 右上方可行走，但是右方不可行走，则当前节点是跳点(拐点)
                // 左下方可行走，但是左方不可行走，则当前节点是跳点(拐点)
                if ((context.isWalkable(currentX + 1, currentY + dy) && !context.isWalkable(currentX + 1, currentY)) ||
                        (context.isWalkable(currentX - 1, currentY + dy) && !context.isWalkable(currentX - 1, currentY))) {
                    return context.getGrid(currentX, currentY);
                }
            }
        }

        // 对角线方向跳跃，要继续对角线移动，必须保证水平方向或垂直方向有一个方向可移动
        // moving diagonally, must make sure one of the vertical/horizontal
        // neighbors is open to allow the path
        if (context.isWalkable(currentX + dx, currentY) || context.isWalkable(currentX, currentY + dy)) {
            return jump(context, currentX, currentY, currentX + dx, currentY + dy);
        } else {
            // 对角线不可以继续移动了（遇见遮挡或越界）
            return null;
        }
    }
}
