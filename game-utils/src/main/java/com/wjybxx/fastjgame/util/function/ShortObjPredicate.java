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

import java.util.Objects;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/9 12:01
 * github - https://github.com/hl845740757
 */
@FunctionalInterface
public interface ShortObjPredicate<V> {

    boolean test(short k, V v);

    default ShortObjPredicate<V> and(ShortObjPredicate<? super V> other) {
        Objects.requireNonNull(other);
        return (k, v) -> test(k, v) && other.test(k, v);
    }

    default ShortObjPredicate<V> or(ShortObjPredicate<? super V> other) {
        Objects.requireNonNull(other);
        return (k, v) -> test(k, v) || other.test(k, v);
    }

    default ShortObjPredicate<V> negate() {
        return (k, v) -> !this.test(k, v);
    }

    static <V> ShortObjPredicate<V> not(ShortObjPredicate<V> predicate) {
        Objects.requireNonNull(predicate);
        return (k, v) -> !predicate.test(k, v);
    }
}
