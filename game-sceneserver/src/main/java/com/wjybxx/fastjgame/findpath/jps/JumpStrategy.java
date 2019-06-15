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

import com.wjybxx.fastjgame.findpath.FindPathNode;
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
    public abstract void findNeighbors(JPSFindPathContext context, FindPathNode curNode, List<MapGrid> neighbors);

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

        final int endX = context.endGrid.getX();
        final int endY = context.endGrid.getY();

        if (dx != 0 && dy != 0){
            // 对角线移动
            return diagonalJump(context, currentX, currentY, dx, dy, endX, endY);
        }else if (dx != 0){
            // 水平跳跃
            return horizontalJump(context, currentX, currentY, dx, endX, endY);
        }else {
            // dy != 0，垂直移动
            return verticalJump(context, currentX, currentY, dy, endX, endY);
        }
    }

    /**
     * 对角线跳跃(moving along diagonal)
     * @param context 寻路上下文
     * @param startX 起点x坐标
     * @param startY 起点y坐标
     * @param dx deltaX x增量
     * @param dy deltaY y增量
     * @param endX 目标节点x坐标
     * @param endY 目标节点y坐标
     * @return jump point(拐点)
     */
    protected abstract MapGrid diagonalJump(JPSFindPathContext context, final int startX, final int startY, int dx, int dy, int endX, int endY);

    /**
     * 水平方向跳跃(moving along x)
     * @param context 寻路上下文
     * @param startX 起点x坐标
     * @param currentY 水平方向移动，y坐标不变
     * @param dx deltaX x增量
     * @param endX 目标节点x坐标
     * @param endY 目标节点y坐标
     * @return jump point(拐点)
     */
    protected abstract MapGrid horizontalJump(JPSFindPathContext context, final int startX, final int currentY, int dx, int endX, int endY);

    /**
     * 垂直方向跳跃(moving along y)
     * @param context 寻路上下文
     * @param currentX 垂直方向移动，x坐标不变
     * @param startY 起点y坐标
     * @param dy deltaY y增量
     * @param endX 目标节点x坐标
     * @param endY 目标节点y坐标
     * @return jump point(拐点)
     */
    protected abstract MapGrid verticalJump(JPSFindPathContext context,final int currentX,final int startY, int dy, int endX, int endY);
}
