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

import com.wjybxx.fastjgame.shape.Point2D;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.List;

/**
 * 寻路算法策略。
 * （基于{@link com.wjybxx.fastjgame.dsl.CoordinateSystem2D}， 标准2维坐标系）
 * <p>
 * (子类实现需要保证为线程安全，以使得将寻路变为task)
 * <p>
 * 咱们不是要做一个支持各种情况的全面的寻路算法，只需要一个符合游戏需求的算法就可以。
 * 考虑的越多，越复杂，性能也会降低；
 * <p>
 * 其它优化：预处理每个地图，索引每种{@link WalkableGridStrategy}所有联通区域;
 * <p>
 * Step1. 当前连通区域编号num初始化为0
 * Step2. 对Grid网格每个点current重复以下工作：
 * 一、 num++。
 * 二、 如果current是阻挡点，跳过。
 * 三、 如果current被访问过，跳过。
 * 四、 current的连通区域编号记为num，标记已访问过。
 * 五、 宽度优先搜索和current四连通的所有Grid网格点，连通区域编号均记为num，
 * 并标记已访问过。
 * </p>
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/6/3 15:18
 * github - https://github.com/hl845740757
 */
@ThreadSafe
public abstract class FindPathStrategy<T extends FindPathContext> {

    /**
     * 计算格子之间距离的函数
     */
    public static final HeuristicFunction distanceFunction = HeuristicFunctions.OCTILE;

    /**
     * 估算消耗(代价)的启发函数
     */
    public static final HeuristicFunction heuristicDistanceFunction = HeuristicFunctions.OCTILE;

    /**
     * 寻找一条可移动的路线
     *
     * @param context 寻路上下文，减少大量的参数传递（参数对象）
     * @return 如果不可达，返回null 或 emptyList
     */
    @Nullable
    public abstract List<Point2D> findPath(T context);

}
