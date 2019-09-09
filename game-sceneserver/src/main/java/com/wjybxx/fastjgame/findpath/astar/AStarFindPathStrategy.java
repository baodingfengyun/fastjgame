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

package com.wjybxx.fastjgame.findpath.astar;

import com.wjybxx.fastjgame.findpath.FindPathStrategy;
import com.wjybxx.fastjgame.shape.Point2D;

import java.util.List;

/**
 * A* 寻路算法
 * <p>
 * A* 算法流程
 * <pre>
 *      G:表示从起点到当前点路径耗费；
 *      H:表示不考虑不可通过区域;
 *      F:表示当前点到终点的理论路径耗费: F=G+H。
 * </pre>
 *
 * <pre>
 *       Step 1. 将起始点start加入开启集合openset
 *       Step 2. 重复以下工作：
 *       一、当openset为空，则结束程序，此时没有路径。
 *       二、寻找openset中F值最小的节点，设为当前点current
 *       三、从openset中移出当前点current
 *       四、关闭集合closedset中加入当前点current
 *       五、若current为目标点goal，则结束程序，此时有路径生成，
 *       此时由goal节点开始逐级追溯路径上每一个节点x的上一级父节点parent(x)，
 *       直至回溯到开始节点start，此时回溯的各节点即为路径。
 *
 *       六、对current的八个方向的每一个相邻点neighbor
 *              1．如果neighbor不可通过或者已经在closedset中，略过。
 *              2．如果neighbor不在openset中，加入openset中
 *              3．如果neighbor在openset中，G值判定，若此路径G值比之前路径小，则neighbor的父节点为current，同时更新G与F值。
 *              反之，则保持原来的节点关系与G、F值。
 *
 * </pre>
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/6/10 23:14
 * github - https://github.com/hl845740757
 */
public class AStarFindPathStrategy extends FindPathStrategy<AStarFindPathContext> {

    @Override
    public List<Point2D> findPath(AStarFindPathContext context) {
        return null;
    }
}
