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

package com.wjybxx.fastjgame.net.serialization;

import com.wjybxx.fastjgame.util.FastCollectionsUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import javax.annotation.Nullable;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/4/21
 */
public class DefaultTypeIdMapper implements TypeIdMapper {

    private final Map<Class<?>, TypeId> type2IdentifierMap;
    private final Long2ObjectMap<Class<?>> number2TypeMap;

    private DefaultTypeIdMapper(Map<Class<?>, TypeId> type2IdentifierMap,
                                Long2ObjectMap<Class<?>> number2TypeMap) {
        this.type2IdentifierMap = type2IdentifierMap;
        this.number2TypeMap = number2TypeMap;
    }

    @Nullable
    @Override
    public TypeId ofType(Class<?> type) {
        return type2IdentifierMap.get(type);
    }

    @Nullable
    @Override
    public Class<?> ofId(TypeId typeId) {
        return number2TypeMap.get(typeId.toGuid());
    }

    public static DefaultTypeIdMapper newInstance(final Set<Class<?>> typeSet, TypeIdMappingStrategy mappingStrategy) {
        final Map<Class<?>, TypeId> type2IdentifierMap = new IdentityHashMap<>(typeSet.size());
        final Long2ObjectMap<Class<?>> number2TypeMap = new Long2ObjectOpenHashMap<>(typeSet.size());

        for (Class<?> type : typeSet) {
            final TypeId typeId = mappingStrategy.mapping(type);

            if (typeId == null) {
                throw new IllegalArgumentException("type " + type + " mapping result is null");
            }

            FastCollectionsUtils.requireNotContains(number2TypeMap, typeId.toGuid(), "id");

            type2IdentifierMap.put(type, typeId);
            number2TypeMap.put(typeId.toGuid(), type);
        }
        return new DefaultTypeIdMapper(type2IdentifierMap, number2TypeMap);
    }

    public static DefaultTypeIdMapper newInstance(final Map<Class<?>, TypeId> src) {
        final Map<Class<?>, TypeId> type2IdentifierMap = new IdentityHashMap<>(src.size());
        final Long2ObjectMap<Class<?>> number2TypeMap = new Long2ObjectOpenHashMap<>(src.size());

        for (Map.Entry<Class<?>, TypeId> entry : src.entrySet()) {
            final Class<?> type = entry.getKey();
            final TypeId typeId = entry.getValue();

            FastCollectionsUtils.requireNotContains(number2TypeMap, typeId.toGuid(), "id");

            type2IdentifierMap.put(type, typeId);
            number2TypeMap.put(typeId.toGuid(), type);
        }

        return new DefaultTypeIdMapper(type2IdentifierMap, number2TypeMap);
    }

}
