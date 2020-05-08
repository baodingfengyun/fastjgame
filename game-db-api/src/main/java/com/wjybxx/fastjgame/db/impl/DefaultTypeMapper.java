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

package com.wjybxx.fastjgame.db.impl;

import com.wjybxx.fastjgame.db.core.TypeIdentifier;
import com.wjybxx.fastjgame.db.core.TypeMapper;
import com.wjybxx.fastjgame.utils.CollectionUtils;
import com.wjybxx.fastjgame.utils.FastCollectionsUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/4/21
 */
public class DefaultTypeMapper implements TypeMapper {

    private final Map<Class<?>, TypeIdentifier> type2IdentifierMap;
    private final Map<String, TypeIdentifier> name2TypeMap;
    private final Long2ObjectMap<TypeIdentifier> number2TypeMap;

    private DefaultTypeMapper(Map<Class<?>, TypeIdentifier> type2IdentifierMap, Map<String, TypeIdentifier> name2TypeMap, Long2ObjectMap<TypeIdentifier> number2TypeMap) {
        this.type2IdentifierMap = type2IdentifierMap;
        this.name2TypeMap = name2TypeMap;
        this.number2TypeMap = number2TypeMap;
    }

    @Nullable
    @Override
    public TypeIdentifier ofType(Class<?> type) {
        return type2IdentifierMap.get(type);
    }

    @Nullable
    @Override
    public TypeIdentifier ofNumber(long number) {
        return number2TypeMap.get(number);
    }

    @Override
    public TypeIdentifier ofName(String name) {
        return name2TypeMap.get(name);
    }

    public static DefaultTypeMapper newInstance(Collection<TypeIdentifier> identifiers) {
        final Map<Class<?>, TypeIdentifier> type2IdentifierMap = new IdentityHashMap<>(identifiers.size());
        final Map<String, TypeIdentifier> name2TypeMap = CollectionUtils.newHashMapWithExpectedSize(identifiers.size());
        final Long2ObjectMap<TypeIdentifier> number2TypeMap = new Long2ObjectOpenHashMap<>(identifiers.size());

        for (TypeIdentifier typeIdentifier : identifiers) {
            CollectionUtils.requireNotContains(type2IdentifierMap, typeIdentifier.type(), "type");
            CollectionUtils.requireNotContains(name2TypeMap, typeIdentifier.uniqueName(), "name");
            FastCollectionsUtils.requireNotContains(number2TypeMap, typeIdentifier.uniqueNumber(), "id");

            type2IdentifierMap.put(typeIdentifier.type(), typeIdentifier);
            name2TypeMap.put(typeIdentifier.uniqueName(), typeIdentifier);
            number2TypeMap.put(typeIdentifier.uniqueNumber(), typeIdentifier);
        }

        return new DefaultTypeMapper(type2IdentifierMap, name2TypeMap, number2TypeMap);
    }

}
