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

package com.wjybxx.fastjgame.net.binary;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.function.IntFunction;

/**
 * 这里仅仅保证有序存储读取的数据，如果出现转型异常或有更具体的序列化需求，请将集合对象放入bean中，
 * 并使用{@link com.wjybxx.fastjgame.db.annotation.Impl}注解提供信息。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/17
 */
class MapCodec {

    MapCodec() {
    }

    static void writeMapImpl(@Nonnull DataOutputStream outputStream,
                             @Nonnull Map<?, ?> value,
                             @Nonnull ObjectWriter writer) throws Exception {
        outputStream.writeInt(value.size());
        if (value.size() == 0) {
            return;
        }

        for (Map.Entry<?, ?> entry : value.entrySet()) {
            writer.writeObject(entry.getKey());
            writer.writeObject(entry.getValue());
        }
    }

    @Nonnull
    static <M extends Map<K, V>, K, V> M readMapImpl(@Nonnull DataInputStream inputStream,
                                                     @Nonnull IntFunction<M> mapFactory,
                                                     @Nonnull ObjectReader reader) throws Exception {
        final int size = inputStream.readInt();
        if (size == 0) {
            return mapFactory.apply(0);
        }

        final M result = mapFactory.apply(size);
        for (int index = 0; index < size; index++) {
            final K key = reader.readObject();
            final V value = reader.readObject();
            result.put(key, value);
        }
        return result;
    }

}
