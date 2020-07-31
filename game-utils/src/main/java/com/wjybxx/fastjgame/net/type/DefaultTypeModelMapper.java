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

package com.wjybxx.fastjgame.net.type;

import com.wjybxx.fastjgame.util.CollectionUtils;
import com.wjybxx.fastjgame.util.FastCollectionsUtils;
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
public class DefaultTypeModelMapper implements TypeModelMapper {

    private final Map<Class<?>, TypeModel> type2IdentifierMap;
    private final Map<String, TypeModel> name2TypeMap;
    private final Long2ObjectMap<TypeModel> number2TypeMap;

    private DefaultTypeModelMapper(Map<Class<?>, TypeModel> type2IdentifierMap, Map<String, TypeModel> name2TypeMap, Long2ObjectMap<TypeModel> number2TypeMap) {
        this.type2IdentifierMap = type2IdentifierMap;
        this.name2TypeMap = name2TypeMap;
        this.number2TypeMap = number2TypeMap;
    }

    @Nullable
    @Override
    public TypeModel ofType(Class<?> type) {
        return type2IdentifierMap.get(type);
    }

    @Override
    public TypeModel ofName(String name) {
        return name2TypeMap.get(name);
    }

    @Nullable
    @Override
    public TypeModel ofId(TypeId typeId) {
        return number2TypeMap.get(typeId.toGuid());
    }

    public static DefaultTypeModelMapper newInstance(Collection<TypeModel> identifiers) {
        final Map<Class<?>, TypeModel> type2IdentifierMap = new IdentityHashMap<>(identifiers.size());
        final Map<String, TypeModel> name2TypeMap = CollectionUtils.newHashMapWithExpectedSize(identifiers.size());
        final Long2ObjectMap<TypeModel> number2TypeMap = new Long2ObjectOpenHashMap<>(identifiers.size());

        for (TypeModel typeModel : identifiers) {
            CollectionUtils.requireNotContains(type2IdentifierMap, typeModel.type(), "type");
            CollectionUtils.requireNotContains(name2TypeMap, typeModel.typeName(), "name");
            FastCollectionsUtils.requireNotContains(number2TypeMap, typeModel.typeId().toGuid(), "id");

            type2IdentifierMap.put(typeModel.type(), typeModel);
            name2TypeMap.put(typeModel.typeName(), typeModel);
            number2TypeMap.put(typeModel.typeId().toGuid(), typeModel);
        }

        return new DefaultTypeModelMapper(type2IdentifierMap, name2TypeMap, number2TypeMap);
    }

}
