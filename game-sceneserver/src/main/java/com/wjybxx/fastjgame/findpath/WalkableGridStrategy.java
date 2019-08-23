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

import com.wjybxx.fastjgame.scene.GridObstacle;

import javax.annotation.concurrent.ThreadSafe;

/**
 * 可行走格子策略；
 *
 * 用于实现特定类型的格子
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/6/10 21:24
 * github - https://github.com/hl845740757
 */
@ThreadSafe
@FunctionalInterface
public interface WalkableGridStrategy {

    /**
     * 该遮挡值的格子是否可以走
     * @param obstacleValue 遮挡值
     * @return true/false
     */
    boolean walkable(GridObstacle obstacleValue);
}
