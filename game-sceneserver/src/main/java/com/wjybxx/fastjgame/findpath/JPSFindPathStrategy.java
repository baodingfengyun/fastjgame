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
import com.wjybxx.fastjgame.shape.Point2D;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * 跳跃点寻路策略
 * (再次注意，我们不是要做一个全面的寻路算法，我们只需要其中的一个或一些情况)
 *
 * <pre>
 * 英文版：
 * - https://zerowidth.com/2013/05/05/jump-point-search-explained.html
 * (强烈建议看看这篇文章，包含跳点寻路 和 A*寻路的展示demo)
 *
 * 中文版：
 * - https://blog.csdn.net/yuxikuo_1/article/details/50406651
 *
 * 此外，在github上可以有很多参考实现。
 *
 * 中文论文：
 * - http://www.doc88.com/p-6099778647749.html
 *
 * 其它优化：
 * - https://cloud.tencent.com/developer/article/1006844
 *
 * 最全寻路算法实现，包括演示demo：
 * (js)
 * - https://github.com/qiao/PathFinding.js
 *
 * java版，代码可读性还是很好的(我这里是实现也主要参考自该项目):
 * - https://github.com/kevinsheehan/jps
 * (java)
 *
 * C#的一个star最多的看的我都要吐了，代码写的太烂。
 *
 * </pre>
 *
 * <h3>跳点</h3>
 * <p>
 *   什么是跳点？存在强制邻居，无法继续跳跃(前进方向的两侧不对称)
 * </p>
 *
 * @author wjybxx
 * @version 1.0
 * @date 2019/6/3 15:34
 * @github - https://github.com/hl845740757
 */
@ThreadSafe
public class JPSFindPathStrategy extends FindPathStrategy<JPSFindPathContext> {

    /**
     * 当前线程令居节点缓存，最多8个邻居
     */
    private static final ThreadLocal<ArrayList<MapGrid>> localNeighbors = ThreadLocal.withInitial(() -> new ArrayList<>(8));

    /**
     * 对角线处理策略
     */
    private static final DiagonalMoveStrategy diagonalMoveStrategy = new AtLeastOneWalkableStrategy();

    /**
     * 路径节点比较器
     * (存为常量，可做优化)
     * (compare F)
     */
    private static final Comparator<FindPathNode> comparator = (a , b) -> Float.compare(a.getF(), b.getF());

    /**
     * 开放节点（未确定最优路径的跳点）
     */
    private static final ThreadLocal<PriorityQueue<FindPathNode>> localOpenNodes = ThreadLocal.withInitial(
            () -> new PriorityQueue<>(64, comparator));
    /**
     * 关闭节点（已确定最优路径的跳点）
     */
    private static final ThreadLocal<ArrayList<FindPathNode>> localCloseNodes = ThreadLocal.withInitial(() -> new ArrayList<>(64));

    @Nullable
    @Override
    public List<Point2D> findPath(JPSFindPathContext context) {
        // 目标节点不可走
        MapGrid endGrid = context.endGrid;
        if (!context.isWalkable(endGrid)) {
            return null;
        }

        PriorityQueue<FindPathNode> openNodes = localOpenNodes.get();
        ArrayList<FindPathNode> closeNodes = localCloseNodes.get();

        final int targetX = endGrid.getX();
        final int targetY = endGrid.getY();

        try {
            // 将初始节点纳入openSet
            FindPathNode firstFindPathNode = new FindPathNode(context.startGrid.getX(), context.startGrid.getY());
            openNodes.add(firstFindPathNode);

            // 最小代价节点，当前可到达的消耗最低的节点(跳点)
            FindPathNode minCostNode;
            while ((minCostNode = openNodes.poll()) != null){
                closeNodes.add(minCostNode);

                if (minCostNode.getX() == targetX && minCostNode.getY() == targetY){
                    // 找到目标点，寻路完成
                    return FindPathUtils.buildFinalPath(context.mapData, minCostNode);
                }

                // 确定下一步
                identifySuccessors(context, minCostNode, openNodes, closeNodes);
            }

            // 寻路失败
            return null;
        }finally {
            openNodes.clear();
            closeNodes.clear();
        }
    }

    private boolean isClosed(ArrayList<FindPathNode> closeNodes, int x, int y){
        for (int index=0,end=closeNodes.size(); index<end; index++){
            FindPathNode pathNode= closeNodes.get(index);
            if (pathNode.getX() == x && pathNode.getY() == y){
                return true;
            }
        }
        return false;
    }

    private FindPathNode findGrid(PriorityQueue<FindPathNode> findPathNodes, int x, int y){
        for (FindPathNode pathNode:findPathNodes){
            if (pathNode.getX() == x && pathNode.getY() == y){
                return pathNode;
            }
        }
        return null;
    }

    private void identifySuccessors(JPSFindPathContext context, FindPathNode curNode,
                                           PriorityQueue<FindPathNode> openNodes, ArrayList<FindPathNode> closeNodes){
        ArrayList<MapGrid> neighbors = localNeighbors.get();
        MapGrid curGrid = context.getGrid(curNode.getX(), curNode.getY());

        float g, h, f;
        MapGrid neighbor, jumpNode;
        FindPathNode existNode, newNode;

        try{
            diagonalMoveStrategy.findNeighbors(context, curNode, neighbors);

            // 朝着可到的邻居方向跳跃(循环遍历次数太多，使用fori的形式性能是最好的)
            for (int index=0,end=neighbors.size();index<end;index++){
                neighbor = neighbors.get(index);

                // curGrid朝着neighbor方向进行跳跃
                jumpNode = diagonalMoveStrategy.jump(context, curNode.getX(), curNode.getY(), neighbor.getX(), neighbor.getY());
                // 遇见遮挡或超出地图了
                if (null == jumpNode){
                    continue;
                }
                // 已存在到达跳点最优路径
                if (isClosed(closeNodes, jumpNode.getX(), jumpNode.getY())){
                    continue;
                }

                g = curNode.getG() + distanceFunction.apply(curGrid, jumpNode);
                h = heuristicDistanceFunction.apply(jumpNode, context.endGrid);
                f = g + h;

                existNode = findGrid(openNodes, jumpNode.getX(), jumpNode.getY());
                if (null == existNode){
                    // 这是一个新的跳点
                    newNode = new FindPathNode(jumpNode.getX(), jumpNode.getY());
                    newNode.setF(f);
                    newNode.setG(g);
                    newNode.setH(h);
                    newNode.setParent(curNode);

                    openNodes.add(newNode);
                }else {
                    // 需要比较到达跳点的消耗，
                    // 如果存在的路径消耗大于新的值，则需要进行替换
                    if (existNode.getG() > g){
                        existNode.setF(f);
                        existNode.setG(g);
                        existNode.setH(h);
                        existNode.setParent(curNode);

                        // 需要删除后重新入堆，否则堆结构会被破坏
                        openNodes.remove(existNode);
                        openNodes.offer(existNode);
                    }
                }
            }
        }finally {
            neighbors.clear();
        }
    }
}
