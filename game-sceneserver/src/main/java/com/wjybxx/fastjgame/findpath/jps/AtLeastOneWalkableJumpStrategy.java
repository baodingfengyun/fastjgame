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
 * 当存在非遮挡格子可达对角线时，可走对角线；
 * 水平和垂直方向至少有一个可行走节点才可以走对角线！
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/6/12 15:54
 * github - https://github.com/hl845740757
 */
public class AtLeastOneWalkableJumpStrategy extends DiagonalJumStrategy {

    @Override
    protected boolean allowMovingAlongDiagonal(boolean horizontalWalkable, boolean verticalWalkable) {
        return horizontalWalkable || verticalWalkable;
    }

    @Override
    protected DiagonalMovement diagonalMovement() {
        return DiagonalMovement.AtLeastOneWalkable;
    }
}
