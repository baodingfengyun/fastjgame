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

import com.wjybxx.fastjgame.net.binary.ObjectReader;
import com.wjybxx.fastjgame.net.binary.ObjectWriter;
import com.wjybxx.fastjgame.net.binary.PojoCodecImpl;

import java.util.Objects;

/**
 * 三元组<br>
 * 三元组已经降低了可读性，再增加元素就是灾难，当需要更多的元素时应该定义自己的类对象。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/7/20
 */
public final class Tuple3<A, B, C> {

    private final Tuple2<A, B> tuple2;
    private final C third;

    public Tuple3(A first, B second, C third) {
        this.tuple2 = new Tuple2<>(first, second);
        this.third = third;
    }

    public Tuple2<A, B> asTuple2() {
        return tuple2;
    }

    public A getFirst() {
        return tuple2.getFirst();
    }

    public B getSecond() {
        return tuple2.getSecond();
    }

    public C getThird() {
        return third;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Tuple3<?, ?, ?> that = (Tuple3<?, ?, ?>) o;

        return tuple2.equals(that.tuple2)
                && Objects.equals(third, that.third);
    }

    @Override
    public final int hashCode() {
        return 31 * tuple2.hashCode() + Objects.hashCode(third);
    }

    @Override
    public String toString() {
        return "Tuple3{" +
                "first=" + tuple2.getFirst() +
                ", second=" + tuple2.getSecond() +
                ", third=" + third +
                '}';
    }

    @SuppressWarnings({"rawtypes", "unused"})
    public static class Tuple3Codec implements PojoCodecImpl<Tuple3> {

        @Override
        public Class<Tuple3> getEncoderClass() {
            return Tuple3.class;
        }

        @Override
        public Tuple3 readObject(ObjectReader reader) throws Exception {
            return new Tuple3<>(reader.readObject(), reader.readObject(), reader.readObject());
        }

        @Override
        public void writeObject(Tuple3 instance, ObjectWriter writer) throws Exception {
            writer.writeObject(instance.getFirst());
            writer.writeObject(instance.getSecond());
            writer.writeObject(instance.getThird());
        }
    }
}
