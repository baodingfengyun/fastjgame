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

package com.wjybxx.fastjgame.util.misc;

import com.wjybxx.fastjgame.net.binary.ObjectReader;
import com.wjybxx.fastjgame.net.binary.ObjectWriter;
import com.wjybxx.fastjgame.net.binary.PojoCodecImpl;

import java.util.Objects;

/**
 * 二元组
 * 注意：虽然提供了元组，但并不提倡大量使用。元组的可读性较差，会增加后期的维护难度，仅当恰好应当使用元组的时候使用元组。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/7/20
 */
public final class Tuple2<A, B> {

    private final A first;
    private final B second;

    public Tuple2(A first, B second) {
        this.first = first;
        this.second = second;
    }

    public A getFirst() {
        return first;
    }

    public B getSecond() {
        return second;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final Tuple2<?, ?> that = (Tuple2<?, ?>) o;
        return Objects.equals(first, that.first)
                && Objects.equals(second, that.second);
    }

    @Override
    public final int hashCode() {
        return 31 * Objects.hashCode(first) + Objects.hashCode(second);
    }

    @Override
    public String toString() {
        return "Tuple2{" +
                "first=" + first +
                ", second=" + second +
                '}';
    }

    @SuppressWarnings({"rawtypes", "unused"})
    private static class Tuple2Codec implements PojoCodecImpl<Tuple2> {

        @Override
        public Class<Tuple2> getEncoderClass() {
            return Tuple2.class;
        }

        @Override
        public Tuple2 readObject(ObjectReader reader) throws Exception {
            return new Tuple2<>(reader.readObject(), reader.readObject());
        }

        @Override
        public void writeObject(Tuple2 instance, ObjectWriter writer) throws Exception {
            writer.writeObject(instance.getFirst());
            writer.writeObject(instance.getSecond());
        }
    }
}
