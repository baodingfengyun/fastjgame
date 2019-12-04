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

package com.wjybxx.fastjgame.misc;

import com.wjybxx.fastjgame.shape.Grid2DContainer;
import com.wjybxx.fastjgame.shape.Point2D;
import com.wjybxx.fastjgame.utils.GameConstant;
import com.wjybxx.fastjgame.utils.MathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.wjybxx.fastjgame.utils.GameConstant.VIEWABLE_DELTA_INDEX;
import static com.wjybxx.fastjgame.utils.GameConstant.VIEWABLE_RANGE_GRID_NUM;

/**
 * 地图的视野格子集合；
 * 真正的视野应该是个圆形，但是无法完美的做到真正的圆形。
 * 而9宫格是比较好的折中。
 * <p>
 * 仔细想下：9宫格的概念其实可以扩展； 25宫格，49宫格...
 * 视野范围分的格子越多，性能开销越大，但控制越精致。
 * <p>
 * 有一篇很好的文章，不过我现在基础的都不熟，别提优化版的了。
 * - https://bbs.gameres.com/thread_827195_1_1.html
 * - https://mp.weixin.qq.com/s/1WN9rA4yK6Wi2-BhQFIn5Q
 * <p>
 * 讲道理只要视野范围相同，小地图是大地图子集的时候，小地图可以复用大地图的视野格子。
 * (也就是说可以回收后再使用，这里突然想起磁盘空间匹配算法....用做那么复杂吗)
 * {@code
 * viewableRange == this.viewableRange &&
 * mapWidth <= this.mapWidth &&
 * mapHeight <= this.mapHeight
 * }
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/31 22:59
 * github - https://github.com/hl845740757
 */
public class ViewGridSet implements Grid2DContainer<ViewGrid> {

    private static final Logger logger = LoggerFactory.getLogger(ViewGridSet.class);

    /**
     * 地图宽高
     */
    private final int mapWidth;
    private final int mapHeight;

    /**
     * 视野半径。
     */
    private final int viewableRange;

    /**
     * 视野格子大小
     */
    private final int viewGridWidth;

    /**
     * 该地图对应的视野格子;
     * [rowIndex][colIndex]
     * 这里需要习惯一下，行对应的y，列对应的是x；
     */
    private final ViewGrid[][] allGrids;

    /**
     * 划分的总行数
     */
    private final int rowCount;

    /**
     * 划分的总列数
     */
    private final int colCount;

    /**
     * @param mapWidth      地图宽
     * @param mapHeight     地图高
     * @param viewableRange 视野大小（视野半径），最好是地图格子大小的倍数
     */
    public ViewGridSet(int mapWidth, int mapHeight, int viewableRange) {
        this.mapWidth = mapWidth;
        this.mapHeight = mapHeight;
        this.viewableRange = viewableRange;
        this.viewGridWidth = (int) (viewableRange / VIEWABLE_RANGE_GRID_NUM);

        // 视野格子宽度最好是整数个地图格子宽度，这样一个地图格子可以完全属于一块视野格子
        if (viewGridWidth % GameConstant.MAP_GRID_WIDTH != 0) {
            logger.warn("bad viewableRange {}", viewableRange);
        }

        // 行数由高度决定，列由宽度决定 (格子宽高一致)
        rowCount = MathUtils.rowCount(mapHeight, viewGridWidth);
        colCount = MathUtils.colCount(mapWidth, viewGridWidth);

        allGrids = new ViewGrid[rowCount][colCount];

        // 初始化视野格子
        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            for (int colIndex = 0; colIndex < colCount; colIndex++) {
                ViewGrid viewGrid = new ViewGrid(rowIndex, colIndex, viewGridWidth);
                allGrids[rowIndex][colIndex] = viewGrid;
            }
        }

        // 索引每个格子可以看见的格子
        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            for (int colIndex = 0; colIndex < colCount; colIndex++) {
                ViewGrid viewGrid = allGrids[rowIndex][colIndex];
                indexViewableGrids(viewGrid, rowIndex, colIndex);
            }
        }
    }

    /**
     * 所有格子可见的格子
     *
     * @param viewGrid       视野格子
     * @param centerRowIndex 视野格子的行索引
     * @param centerColIndex 视野格子的列索引
     */
    private void indexViewableGrids(ViewGrid viewGrid, final int centerRowIndex, final int centerColIndex) {
        // 行间距差值
        for (int deltaRow = -VIEWABLE_DELTA_INDEX; deltaRow <= VIEWABLE_DELTA_INDEX; deltaRow++) {
            int curRowIndex = centerRowIndex + deltaRow;
            // 行溢出
            if (curRowIndex < 0 || curRowIndex >= rowCount) {
                continue;
            }
            // 列间距差值
            for (int deltaCol = -VIEWABLE_DELTA_INDEX; deltaCol <= VIEWABLE_DELTA_INDEX; deltaCol++) {
                int curColIndex = centerColIndex + deltaCol;
                // 列溢出
                if (curColIndex < 0 || curColIndex >= colCount) {
                    continue;
                }
                viewGrid.getViewableGrids().add(allGrids[curRowIndex][curColIndex]);
            }
        }
    }

    public int getMapWidth() {
        return mapWidth;
    }

    public int getMapHeight() {
        return mapHeight;
    }

    @Override
    public ViewGrid[][] getAllGrids() {
        return allGrids;
    }

    @Override
    public int getRowCount() {
        return rowCount;
    }

    @Override
    public int getColCount() {
        return colCount;
    }

    public int getViewableRange() {
        return viewableRange;
    }

    public int getViewGridWidth() {
        return viewGridWidth;
    }

    /**
     * 查询地图坐标是否在地图视野格子内
     * （该方法应该用不上）
     *
     * @param point2D 坐标
     * @return 视野格子
     */
    public boolean inside(Point2D point2D) {
        return MathUtils.withinRangeClosed(0, mapWidth, (int) point2D.getX()) &&
                MathUtils.withinRangeClosed(0, mapHeight, (int) point2D.getY());
    }

    /**
     * 查找指定坐标所属的视野格子
     *
     * @param point2D 坐标
     * @return 视野格子
     */
    public ViewGrid getGrid(Point2D point2D) {
        int rowIndex = MathUtils.rowIndex(rowCount, viewGridWidth, point2D.getY());
        int colIndex = MathUtils.colIndex(colCount, viewGridWidth, point2D.getX());
        return allGrids[rowIndex][colIndex];
    }

    /**
     * 视野管理的时候，是否可见，取决于视野格子是否挨着。
     * 这里是可见性 和 常见游戏失明那种效果不是一回事。
     * (失明效果只是单纯的特效表现，并不影响我周围到底存在哪些单位)
     * <p>
     * 如果格子是缓存的对象，则该方法比较的并不是实时的可见性；
     * 由于游戏对象可能频繁的移动，如果总是根据实时坐标构建视野，那会性能消耗惊人，
     * 因此往往视野格子可能会缓存，降低视野的更新频率；
     *
     * @param a 视野格子
     * @param b 另一个视野格子
     * @return
     */
    public boolean visible(ViewGrid a, ViewGrid b) {
        // 行列索引差值都小于等于1
        return Math.abs(a.getRowIndex() - b.getRowIndex()) <= VIEWABLE_DELTA_INDEX &&
                Math.abs(a.getColIndex() - b.getColIndex()) <= VIEWABLE_DELTA_INDEX;
    }

    /**
     * 查询两个点是否可见（实时）
     *
     * @param a 坐标1
     * @param b 坐标2
     * @return
     */
    public boolean visible(Point2D a, Point2D b) {
        return visible(getGrid(a), getGrid(b));
    }

    @Override
    public String toString() {
        return "ViewGridSet{" +
                "mapWidth=" + mapWidth +
                ", mapHeight=" + mapHeight +
                ", viewableRange=" + viewableRange +
                ", viewGridWidth=" + viewGridWidth +
                ", rowCount=" + rowCount +
                ", colCount=" + colCount +
                '}';
    }
}
