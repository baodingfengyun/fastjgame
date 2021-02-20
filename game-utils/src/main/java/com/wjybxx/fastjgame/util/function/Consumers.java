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

/**
 * 常用{@link java.util.function.Consumer}类型函数
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/9/12
 * github - https://github.com/hl845740757
 */
public interface Consumers {

    @FunctionalInterface
    interface Consumer3<A, B, C> {

        void accept(A a, B b, C c);

    }

    @FunctionalInterface
    interface Consumer4<A, B, C, D> {

        void accept(A a, B b, C c, D d);

    }

    @FunctionalInterface
    interface Consumer5<A, B, C, D, E> {

        void accept(A a, B b, C c, D d, E e);

    }

    @FunctionalInterface
    interface Consumer6<A, B, C, D, E, F> {

        void accept(A a, B b, C c, D d, E e, F f);

    }

    @FunctionalInterface
    interface Consumer7<A, B, C, D, E, F, G> {

        void accept(A a, B b, C c, D d, E e, F f, G g);

    }

}
