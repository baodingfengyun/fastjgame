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

package com.wjybxx.fastjgame.util.function;

import it.unimi.dsi.fastutil.shorts.ShortConsumer;

import java.util.function.*;

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
    public static final Runnable NO_OP_TASK = () -> {
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

    private static final Predicate<?> _alwaysTrue = v -> true;

    private static final Predicate<?> _alwaysFalse = v -> false;

    private static final BiPredicate<?, ?> _alwaysTrue2 = (t, u) -> true;

    private static final BiPredicate<?, ?> _alwaysFalse2 = (t, u) -> false;

    private static final Function<?, ?> identity = t -> t;

    @SuppressWarnings("unchecked")
    public static <T> Function<T, T> identity() {
        return (Function<T, T>) identity;
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

    @SuppressWarnings("unchecked")
    public static <T> Predicate<T> alwaysTrue() {
        return (Predicate<T>) _alwaysTrue;
    }

    @SuppressWarnings("unchecked")
    public static <T> Predicate<T> alwaysFalse() {
        return (Predicate<T>) _alwaysFalse;
    }

    @SuppressWarnings("unchecked")
    public static <T, U> BiPredicate<T, U> alwaysTrue2() {
        return (BiPredicate<T, U>) _alwaysTrue2;
    }

    @SuppressWarnings("unchecked")
    public static <T, U> BiPredicate<T, U> alwaysFalse2() {
        return (BiPredicate<T, U>) _alwaysFalse2;
    }

}
