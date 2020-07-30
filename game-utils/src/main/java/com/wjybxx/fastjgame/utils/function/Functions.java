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

package com.wjybxx.fastjgame.utils.function;

/**
 * 常用{@link java.util.function.Function}类型函数
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/9/12
 * github - https://github.com/hl845740757
 */
public interface Functions {

    @FunctionalInterface
    interface Function3<R, A, B, C> {

        R apply(A a, B b, C c);

    }

    @FunctionalInterface
    interface Function4<R, A, B, C, D> {

        R apply(A a, B b, C c, D d);

    }

    @FunctionalInterface
    interface Function5<R, A, B, C, D, E> {

        R apply(A a, B b, C c, D d, E e);

    }

    @FunctionalInterface
    interface Function6<R, A, B, C, D, E, F> {
        R apply(A a, B b, C c, D d, E e, F f);
    }

    @FunctionalInterface
    interface Function7<R, A, B, C, D, E, F, G> {
        R apply(A a, B b, C c, D d, E e, F f, G g);
    }
}
