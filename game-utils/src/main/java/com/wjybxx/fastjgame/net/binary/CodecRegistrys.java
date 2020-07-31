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

import com.wjybxx.fastjgame.net.type.TypeId;
import com.wjybxx.fastjgame.net.type.TypeModel;
import com.wjybxx.fastjgame.net.type.TypeModelMapper;
import com.wjybxx.fastjgame.utils.CollectionUtils;
import com.wjybxx.fastjgame.utils.FastCollectionsUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import java.util.IdentityHashMap;
import java.util.List;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/25
 */
public class CodecRegistrys {

    public static CodecRegistry fromAppPojoCodecs(TypeModelMapper typeModelMapper, List<PojoCodecImpl<?>> pojoCodecs) {
        final Long2ObjectMap<PojoCodecImpl<?>> typeId2CodecMap = new Long2ObjectOpenHashMap<>(pojoCodecs.size());
        final IdentityHashMap<Class<?>, PojoCodecImpl<?>> type2CodecMap = new IdentityHashMap<>(pojoCodecs.size());

        for (PojoCodecImpl<?> pojoCodec : pojoCodecs) {
            final Class<?> type = pojoCodec.getEncoderClass();
            final TypeModel typeModel = typeModelMapper.ofType(type);

            if (typeModel == null) {
                continue;
            }

            FastCollectionsUtils.requireNotContains(typeId2CodecMap, typeModel.typeId().toGuid(), "typeId-(toGuid)");
            CollectionUtils.requireNotContains(type2CodecMap, type, "type");

            typeId2CodecMap.put(typeModel.typeId().toGuid(), pojoCodec);
            type2CodecMap.put(type, pojoCodec);
        }

        return new DefaultCodecRegistry(typeModelMapper, typeId2CodecMap, type2CodecMap);
    }

    private static class DefaultCodecRegistry implements CodecRegistry {

        private final TypeModelMapper typeModelMapper;
        private final Long2ObjectMap<PojoCodecImpl<?>> typeId2CodecMap;
        private final IdentityHashMap<Class<?>, PojoCodecImpl<?>> type2CodecMap;

        private DefaultCodecRegistry(TypeModelMapper typeModelMapper,
                                     Long2ObjectMap<PojoCodecImpl<?>> typeId2CodecMap,
                                     IdentityHashMap<Class<?>, PojoCodecImpl<?>> type2CodecMap) {
            this.typeModelMapper = typeModelMapper;
            this.typeId2CodecMap = typeId2CodecMap;
            this.type2CodecMap = type2CodecMap;
        }

        @Override
        public TypeModelMapper typeModelMapper() {
            return typeModelMapper;
        }

        @Override
        public <T> PojoCodecImpl<T> get(Class<T> clazz) {
            @SuppressWarnings("unchecked") PojoCodecImpl<T> codec = (PojoCodecImpl<T>) type2CodecMap.get(clazz);
            return codec;
        }

        @Override
        public <T> PojoCodecImpl<T> get(TypeId typeId) {
            @SuppressWarnings("unchecked") PojoCodecImpl<T> codec = (PojoCodecImpl<T>) typeId2CodecMap.get(typeId.toGuid());
            return codec;
        }
    }

}
