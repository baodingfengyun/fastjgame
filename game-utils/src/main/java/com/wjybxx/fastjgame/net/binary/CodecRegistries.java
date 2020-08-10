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

import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/25
 */
public class CodecRegistries {

    public static CodecRegistry fromPojoCodecs(Map<Class<?>, PojoCodec<?>>... pojoCodecMaps) {
        final int sum = Arrays.stream(pojoCodecMaps)
                .mapToInt(Map::size)
                .sum();

        final IdentityHashMap<Class<?>, PojoCodec<?>> identityHashMap = new IdentityHashMap<>(sum);
        for (Map<Class<?>, PojoCodec<?>> map : pojoCodecMaps) {
            identityHashMap.putAll(map);
        }

        if (identityHashMap.size() != sum) {
            throw new IllegalArgumentException("some type has more than one codec");
        }

        return new DefaultCodecRegistry(identityHashMap);
    }

    private static class DefaultCodecRegistry implements CodecRegistry {

        private final IdentityHashMap<Class<?>, PojoCodec<?>> type2CodecMap;

        private DefaultCodecRegistry(IdentityHashMap<Class<?>, PojoCodec<?>> type2CodecMap) {
            this.type2CodecMap = type2CodecMap;
        }

        @Override
        public <T> PojoCodec<T> get(Class<T> clazz) {
            @SuppressWarnings("unchecked") PojoCodec<T> codec = (PojoCodec<T>) type2CodecMap.get(clazz);
            return codec;
        }

    }
}