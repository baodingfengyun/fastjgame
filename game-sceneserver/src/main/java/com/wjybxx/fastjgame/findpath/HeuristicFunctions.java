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

/**
 * 常用启发函数；
 *
 * <pre>
 * 对于网格地图来说, 如果只能四方向(上下左右)移动, 曼哈顿距离(Manhattan distance) 是最合适的启发函数.
 * {@code
 *  function Manhattan(node) =
 *      dx = abs(node.x - goal.x)
 *      dy = abs(node.y - goal.y)
 *      // 在最简单的情况下, D 可以取 1, 返回值即 dx + dy
 *      return D * (dx + dy)
 * }
 * </pre>
 *
 * <pre>
 * 如果网格地图可以八方向(包括斜对角)移动, 使用 切比雪夫距离(Chebyshev distance) 作为启发函数比较合适.
 * {@code
 * function Chebyshev(node) =
 *     dx = abs(node.x - goal.x)
 *     dy = abs(node.y - goal.y)
 *     // max(dx, dy) 保证了斜对角的距离计算
 *     return D * max(dx, dy)
 * }
 * </pre>
 *
 * <pre>
 * 如果地图中允许任意方向移动, 不太建议使用网格 (Grid) 来描述地图, 可以考虑使用 路点 (Way Points) 或者
 * 导航网格 (Navigation Meshes) , 此时使用 欧式距离(Euclidean distance) 来作为启发函数比较合适.
 *
 * {@code
 * function heuristic(node) =
 *     dx = abs(node.x - goal.x)
 *     dy = abs(node.y - goal.y)
 *     // 在最简单的情况下, D 可以取 1, 返回值即 sqrt(dx * dx + dy * dy)
 *     return D * sqrt(dx * dx + dy * dy)
 * }
 * </pre>
 *
 *
 * <pre>
 * 欧式距离因为有一个 sqrt() 运算, 计算开销会增大, 所以可以使用 Octile 距离 来优化(不知道怎么翻译),
 * Octile 的核心思想就是假定只能做 45 度角转弯.
 * {@code
 * function heuristic(node) =
 *     dx = abs(node.x - goal.x)
 *     dy = abs(node.y - goal.y)
 *     k = sqrt(2) - 1
 *     return max(dx, dy) + k * min(dx, dy)
 * }
 *</pre>
 * - https://www.cnblogs.com/sanmubai/p/6829495.html
 *
 * @author wjybxx
 * @version 1.0
 * @date 2019/6/10 19:30
 * @github - https://github.com/hl845740757
 */
public class HeuristicFunctions {

    private HeuristicFunctions() {
    }

    /**
     * 曼哈顿距离(代价)
     */
    public static final HeuristicFunction MANHATTAN = (a, b) -> {
        int dx = Math.abs(a.getX() - b.getX());
        int dy = Math.abs(a.getY() - b.getY());
        return dx + dy;
    };

    /**
     * 切比雪夫距离(代价)
     */
    public static final HeuristicFunction CHEBYSHEV = (a, b) -> {
        int dx = Math.abs(a.getX() - b.getX());
        int dy = Math.abs(a.getY() - b.getY());
        return Math.max(dx,dy);
    };

    /**
     * 欧几里得距离(代价)
     */
    public static final HeuristicFunction EUCLIDEAN = (a, b) -> {
        int dx = Math.abs(a.getX() - b.getX());
        int dy = Math.abs(a.getY() - b.getY());
        return (float) Math.sqrt(dx * dx + dy * dy);
    };


    private static final float OCTILE_K = (float) (Math.sqrt(2) - 1);
    /**
     * 不知道怎么翻译（它是对欧几里得sqrt操作的一个优化）
     * <pre>
     * {@code
     *     dx = abs(node.x - goal.x)
     *     dy = abs(node.y - goal.y)
     *     k = sqrt(2) - 1
     *     return max(dx, dy) + k * min(dx, dy)
     * }
     * </pre>
     */
    public static final HeuristicFunction OCTILE = (a, b) -> {
        int dx = Math.abs(a.getX() - b.getX());
        int dy = Math.abs(a.getY() - b.getY());
        return dx < dy ? OCTILE_K * dx + dy : OCTILE_K * dy + dx;
    };

}
