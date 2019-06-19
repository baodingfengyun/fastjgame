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
import com.wjybxx.fastjgame.misc.Stateless;
import com.wjybxx.fastjgame.scene.MapGrid;

import java.util.List;

/**
 * 跳跃策略；
 * 跳点寻路时如何处理对角线问题;
 * 子类必须是无状态的，以实现线程安全;
 * @author wjybxx
 * @version 1.0
 * @date 2019/6/12 14:22
 * @github - https://github.com/hl845740757
 */
@Stateless
public abstract class JumpStrategy {

    /**
     * 获取当前节点的邻居节点
     * @param context jps寻路上下文
     * @param curNode 当前节点
     * @param neighbors 邻居节点容器，传入以方便子类使用缓存
     */
    public final void findNeighbors(JPSFindPathContext context, FindPathNode curNode, List<MapGrid> neighbors){
        final int x = curNode.getX();
        final int y = curNode.getY();

        FindPathNode parent = curNode.getParent();
        if (parent != null) {
            // 父节点不为null，表示当前沿特定方向移动，返回该特定方向的邻居节点
            // return special direction neighbors

            // get normalized direction of travel
            // 归一化为水平和垂直方向的单位向量(-1,0,1)，用于判断当前节点的探测方向，我居然想到了compare这个骚操作
            // 因为跳点不一定是连续的，因此需要对dx dy进行处理
            final int dx = Integer.compare(x, parent.getX());
            final int dy = Integer.compare(y, parent.getY());

            if (dx != 0 && dy != 0) {
                // moving along the diagonal, 对角线移动，查找对角线方向邻居
                findDiagonalNeighbors(context, x, y, dx, dy, neighbors);
            } else {
                if (dx != 0){
                    // dx != 0 moving along x, 水平方向移动，查找水平方向邻居
                    findHorizontalNeighbors(context, x, y, dx, neighbors);
                } else {
                    // dy != 0 moving along y, 垂直方向移动，查找垂直方向邻居
                    findVerticalNeighbors(context, x, y, dy, neighbors);
                }
            }
        } else {
            // parent为null，表示初始节点，返回所有的邻居节点
            // no parent, return all neighbors
            FindPathUtils.statisticsNeighbors(context, x, y, diagonalMovement(), neighbors);
        }
    }

    /**
     * 查找对角线方向邻居;
     * 正常邻居有三个，水平方向一个，垂直方向一个，对角线方向一个，
     * 还需要检查两处强迫邻居，左上邻居 和 右下邻居。
     * @param context jps寻路上下文
     * @param x 当前x坐标
     * @param y 当前y坐标
     * @param dx deltaX x增量
     * @param dy deltaY y增量
     * @param neighbors 邻居节点容器，传入以方便子类使用缓存
     */
    protected abstract void findDiagonalNeighbors(JPSFindPathContext context, int x, int y, int dx, int dy, List<MapGrid> neighbors);

    /**
     * 查找水平方向邻居
     * @param context jps寻路上下文
     * @param x 当前x坐标
     * @param y 当前y坐标
     * @param dx deltaX x增量
     * @param neighbors 邻居节点容器，传入以方便子类使用缓存
     */
    protected abstract void findHorizontalNeighbors(JPSFindPathContext context, int x, int y, int dx, List<MapGrid> neighbors);

    /**
     * 查找垂直方向邻居
     * @param context jps寻路上下文
     * @param x 当前x坐标
     * @param y 当前y坐标
     * @param dy deltaY y增量
     * @param neighbors 邻居节点容器，传入以方便子类使用缓存
     */
    protected abstract void findVerticalNeighbors(JPSFindPathContext context, int x, int y, int dy, List<MapGrid> neighbors);

    /**
     * 返回该策略对应的对角线移动特征值
     * @return {@link DiagonalMovement}
     */
    protected abstract DiagonalMovement diagonalMovement();

    /**
     * 从curNode到neighbor方向进行跳跃，直到遇见跳点 或 遮挡 或 越界；
     * 遇见遮挡或越界表示该方向没有意义，返回null即可，
     * 遇见跳点，表示需要将跳点纳入探测范围；
     * (用for循环代替了递归)
     *
     * @param context jps寻路上下文
     * @param parentX 父节点x坐标
     * @param parentY 父节点y坐标
     * @param currentX 当前x坐标
     * @param currentY 当前y坐标
     * @return jump point（换个说法，拐点）
     */
    public final MapGrid jump(JPSFindPathContext context,int parentX, int parentY, int currentX, int currentY){
        // 当前节点与父节点的x和y增量区间为[-1,0,1]
        final int dx = currentX - parentX;
        final int dy = currentY - parentY;

        if (dx != 0 && dy != 0){
            // 对角线移动
            return diagonalJump(context, currentX, currentY, dx, dy);
        }else if (dx != 0){
            // 水平跳跃
            return horizontalJump(context, currentX, currentY, dx);
        }else {
            // dy != 0，垂直移动
            return verticalJump(context, currentX, currentY, dy);
        }
    }

    /**
     * 对角线跳跃(moving along diagonal)
     * （我们以向右上方（东北方向）行走进行表述）
     * @param context 寻路上下文
     * @param startX 起点x坐标
     * @param startY 起点y坐标
     * @param dx deltaX x增量
     * @param dy deltaY y增量
     * @return jump point(拐点)
     */
    protected abstract MapGrid diagonalJump(JPSFindPathContext context, final int startX, final int startY, int dx, int dy);

    /**
     * 水平方向跳跃(moving along x)
     * （我们以向右（东方）走进行表述）
     * @param context 寻路上下文
     * @param startX 起点x坐标
     * @param currentY 水平方向移动，y坐标不变
     * @param dx deltaX x增量
     * @return jump point(拐点)
     */
    protected abstract MapGrid horizontalJump(JPSFindPathContext context, final int startX, final int currentY, int dx);

    /**
     * 垂直方向跳跃(moving along y)
     * （我们以向上（北方）走进行表述）
     * @param context 寻路上下文
     * @param currentX 垂直方向移动，x坐标不变
     * @param startY 起点y坐标
     * @param dy deltaY y增量
     * @return jump point(拐点)
     */
    protected abstract MapGrid verticalJump(JPSFindPathContext context, final int currentX, final int startY, int dy);
}
