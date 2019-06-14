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
 * @author wjybxx
 * @version 1.0
 * @date 2019/6/12 18:54
 * @github - https://github.com/hl845740757
 */
public class NeighborOffSet {

    /**
     * x偏移量
     */
    final int xOffset;

    /**
     * y偏移量
     */
    final int yOffset;

    /**
     * 是否是对角线
     */
    final boolean diagonal;

    public NeighborOffSet(int xOffset, int yOffset) {
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        // x或y为0的是上下左右，偏移量绝对值相同的是对角线
        this.diagonal = Math.abs(xOffset) == Math.abs(yOffset);
    }

    public int getXOffset() {
        return xOffset;
    }

    public int getYOffset() {
        return yOffset;
    }

    public boolean isDiagonal() {
        return diagonal;
    }
}
