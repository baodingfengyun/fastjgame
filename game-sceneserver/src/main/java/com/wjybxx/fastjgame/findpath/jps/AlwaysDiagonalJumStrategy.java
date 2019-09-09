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

/**
 * 只要对角线非遮挡，总是允许走对角线
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/6/16 17:27
 * github - https://github.com/hl845740757
 */
public class AlwaysDiagonalJumStrategy extends DiagonalJumStrategy {

    /**
     * 对角线非遮挡时，总是允许对角线移动
     *
     * @param horizontalWalkable 水平方向是否可行走
     * @param verticalWalkable   垂直方向是否可行走
     * @return
     */
    @Override
    protected boolean allowMovingAlongDiagonal(boolean horizontalWalkable, boolean verticalWalkable) {
        return true;
    }

    @Override
    protected DiagonalMovement diagonalMovement() {
        return DiagonalMovement.Always;
    }
}
