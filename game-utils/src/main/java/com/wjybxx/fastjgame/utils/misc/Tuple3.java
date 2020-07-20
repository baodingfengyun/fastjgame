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

package com.wjybxx.fastjgame.utils.misc;

import java.util.Objects;

/**
 * 三元组
 * <p>
 * Q: 为什么定义为final类？
 * A: 三元组已经降低了可读性，再增加元素就是灾难，当需要更多的元素时应该定义自己的类对象。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/7/20
 */
public final class Tuple3<A, B, C> extends Tuple2<A, B> {

    public final C third;

    public Tuple3(A first, B second, C third) {
        super(first, second);
        this.third = third;
    }

    public C getThird() {
        return third;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        if (!super.equals(o)) {
            return false;
        }

        Tuple3<?, ?, ?> that = (Tuple3<?, ?, ?>) o;
        return Objects.equals(third, that.third);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(third);
    }
}
