/*
 *  Copyright 2019 wjybxx
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to iBn writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.wjybxx.fastjgame.utils;

import java.util.Objects;

/**
 * 检查工具类
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/10/8
 * github - https://github.com/hl845740757
 */
public class CheckUtils {

    private CheckUtils() {
    }

    /**
     * 检查一个数是否是正数
     *
     * @param param 参数
     * @param name  属性的名字
     */
    public static void checkPositive(final int param, String name) {
        if (param <= 0) {
            throw new IllegalArgumentException(name + ": " + param + " (expected: > 0)");
        }
    }

    public static void checkPositive(final long param, String name) {
        if (param <= 0) {
            throw new IllegalArgumentException(name + ": " + param + " (expected: > 0)");
        }
    }

    /**
     * 检查一个参数是否不为null
     *
     * @param param 参数
     * @param name  属性的名字
     */
    public static void checkNonNull(final Object param, String name) {
        Objects.requireNonNull(param, name);
    }
}
