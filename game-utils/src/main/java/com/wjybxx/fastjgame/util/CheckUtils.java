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

package com.wjybxx.fastjgame.util;

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

    public static String requireNotNullAndNotEmpty(final String value, final String name) {
        Objects.requireNonNull(value, name);

        if (value.isEmpty()) {
            throw new IllegalArgumentException(name + " is empty");
        }

        return value;
    }

    /**
     * 检查一个数是否为正数。如果是，则返回它；否则抛出异常。
     *
     * @param param 参数
     * @param name  属性的名字
     * @return param
     */
    public static int requirePositive(final int param, String name) {
        if (param <= 0) {
            throw new IllegalArgumentException(name + ": " + param + " (expected: > 0)");
        }
        return param;
    }

    /**
     * 检查一个数是否为正数。如果是，则返回它；否则抛出异常。
     *
     * @param param 参数
     * @param name  属性的名字
     * @return param
     */
    public static long requirePositive(final long param, String name) {
        if (param <= 0) {
            throw new IllegalArgumentException(name + ": " + param + " (expected: > 0)");
        }
        return param;
    }

}
