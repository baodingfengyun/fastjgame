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

/**
 * 启发函数;
 *
 * 盲目搜索会浪费很多时间和空间, 所以我们在路径搜索时, 会首先选择最有希望的节点,
 * 这种搜索称之为 "启发式搜索 (Heuristic Search)"
 *
 * 如何来界定"最有希望"? 我们需要通过 启发函数 (Heuristic Function) 计算得到.
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/6/10 19:30
 * github - https://github.com/hl845740757
 */
@FunctionalInterface
public interface HeuristicFunction {

    float apply(MapGrid a, MapGrid b);

}
