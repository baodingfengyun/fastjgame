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

import it.unimi.dsi.fastutil.shorts.ShortConsumer;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

/**
 * 常用函数式方法
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/4
 * github - https://github.com/hl845740757
 */
public final class FunctionUtils {

    /**
     * 什么也不做的Action
     */
    public static final Runnable NO_OP_ACTION = () -> {
    };

    private static final BiConsumer<?, ?> _emptyBiConsumer = (a, b) -> {
    };

    private static final Consumer<?> _emptyConsumer = v -> {
    };

    private static final ShortConsumer _emptyShortConsumer = v -> {
    };

    private static final IntConsumer _emptyIntConsumer = v -> {
    };

    private static final LongConsumer _emptyLongConsumer = v -> {
    };


    private FunctionUtils() {

    }

    @SuppressWarnings("unchecked")
    public static <T, U> BiConsumer<T, U> emptyBiConsumer() {
        return (BiConsumer<T, U>) _emptyBiConsumer;
    }

    @SuppressWarnings("unchecked")
    public static <T> Consumer<T> emptyConsumer() {
        return (Consumer<T>) _emptyConsumer;
    }

    public static ShortConsumer emptyShortConsumer() {
        return _emptyShortConsumer;
    }

    public static IntConsumer emptyIntConsumer() {
        return _emptyIntConsumer;
    }

    public static LongConsumer emptyLongConsumer() {
        return _emptyLongConsumer;
    }

    public static <T> boolean TRUE(T t) {
        return true;
    }

    public static <T> boolean FALSE(T t) {
        return false;
    }

    // ---------------------------------- obj - obj -----------------------------
    public static <T, U> boolean TRUE(T t, U u) {
        return true;
    }

    public static <T, U> boolean FALSE(T t, U u) {
        return false;
    }

}
