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
 * 对角线移动类型
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/6/10 21:20
 * github - https://github.com/hl845740757
 */
public enum DiagonalMovement {

    /**
     * 禁止对角线移动
     */
    Never,

    /**
     * 总是允许对角线移动，即使两边都是遮挡
     * <pre>
     *     |-----------|
     *     |B(X) | D   |
     *     ------------|
     *     | A   |C(X) |
     *     |-----------|
     *         (√)
     * </pre>
     */
    Always,

    /**
     * 至少有一个可移动的格子的时候
     * (对角线两边格子至少有一个可行走)
     *
     * <pre>
     *     |---------|
     *     |B(X)| D  |
     *     ----------|
     *     | A  | C  |
     *     |---------|
     *         (√)
     * 如图，B不可以行走，但C可以行走，因此可以走对角线，直接A -> D
     *
     *     |-----------|
     *     |B(X) | D   |
     *     ------------|
     *     | A   |C(X) |
     *     |-----------|
     *         (X)
     * 如图，B和C都不可以行走，因此不可以走对角线
     * </pre>
     */
    AtLeastOneWalkable,

    /**
     * 仅当没有障碍物格子的时候
     * (对角线两边格子必须都可行走)
     *
     * <pre>
     *     |---------|
     *     | B  | D  |
     *     ----------|
     *     | A  | C  |
     *     |---------|
     *        (√)
     * 如图，如果B,C都可以行走，则可以直接走D
     *
     *     |---------|
     *     |B(X)| D  |
     *     ----------|
     *     | A  | C  |
     *     |---------|
     *        (X)
     * 如图，由于B不可以行走，因此不可以走对角线
     * </pre>
     */
    OnlyWhenNoObstacles;
}
