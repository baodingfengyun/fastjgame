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

package com.wjybxx.fastjgame.utils;

/**
 * 常用函数式方法
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/4
 * github - https://github.com/hl845740757
 */
public final class FunctionUtils {

    /** 什么也不做的Action */
    public static final Runnable NO_OP_ACTION = () -> {};

    private FunctionUtils() {

    }

    public static <T> boolean TRUE(T t) {
        return true;
    }

    public static <T> boolean FALSE(T t) {
        return false;
    }

    // ---------------------------------- obj - obj -----------------------------
    public static <T,U> boolean TRUE(T t, U u) {
        return true;
    }

    public static <T,U> boolean FALSE(T t, U u) {
        return false;
    }

    // ---------------------------------- int - obj -----------------------------
    public static <T> boolean TRUE(int a, T b) {
        return true;
    }

    public static <T> boolean FALSE(int a, T b) {
        return false;
    }

    // ---------------------------------- long - obj -----------------------------
    public static <T> boolean TRUE(long a, T b) {
        return true;
    }

    public static <T> boolean FALSE(long a, T b) {
        return false;
    }
}
