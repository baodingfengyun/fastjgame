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

package com.wjybxx.fastjgame.shape;

/**
 * 2D格子
 * @author wjybxx
 * @version 1.0
 * @date 2019/6/10 20:18
 * @github - https://github.com/hl845740757
 */
public interface Grid2D {

    /**
     * 获取格子的行索引
     */
    int getRowIndex();

    /**
     * 获取格子的列索引
     */
    int getColIndex();

    /**
     * 当进行数学计算的时候，我们更习惯使用XY，而不是行列；
     * X 就是 列索引
     * Y 就是 行索引
     */
    default int getX(){
        return getColIndex();
    }

    /**
     * 当进行数学计算的时候，我们更习惯使用XY，而不是行列；
     * X 就是 列索引
     * Y 就是 行索引
     */
    default int getY(){
        return getRowIndex();
    }
}
