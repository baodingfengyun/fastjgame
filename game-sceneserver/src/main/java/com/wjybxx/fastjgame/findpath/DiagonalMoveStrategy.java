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

import com.wjybxx.fastjgame.misc.Stateless;
import com.wjybxx.fastjgame.scene.MapGrid;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 跳点寻路时如何处理对角线问题;
 * 子类必须是无状态的，以实现线程安全;
 * @author wjybxx
 * @version 1.0
 * @date 2019/6/12 14:22
 * @github - https://github.com/hl845740757
 */
@Stateless
public interface DiagonalMoveStrategy {

    /**
     * 获取当前节点的邻居节点
     * @param context jps寻路上下文
     * @param curNode 当前节点
     * @param neighbors 邻居节点容器，传入以方便子类使用缓存
     */
    void findNeighbors(JPSFindPathContext context, FindPathNode curNode, List<MapGrid> neighbors);

    /**
     * 从curNode到neighbor方向进行跳跃，直到遇见跳点 或 遮挡 或 越界；
     * 遇见遮挡或越界表示该方向没有意义，返回null即可，
     * 遇见跳点，表示需要将跳点纳入探测范围；
     *
     * @param context jps寻路上下文
     * @param parentX parentX
     * @param parentY parentY
     * @param currentX
     * @param currentY
     * @return jump point（换个说法，拐点）
     */
    @Nullable
    MapGrid jump(JPSFindPathContext context, int parentX, int parentY, int currentX, int currentY);
}
