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

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import javax.annotation.Nullable;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/24
 */
public class AppCodecProvider implements PojoCodecProvider {

    private final Map<Class<?>, AppPojoCodec<?>> type2CodecMap;
    private final Int2ObjectMap<AppPojoCodec<?>> classId2CodecMap;

    private AppCodecProvider(Map<Class<?>, AppPojoCodec<?>> type2CodecMap, Int2ObjectMap<AppPojoCodec<?>> classId2CodecMap) {
        this.type2CodecMap = type2CodecMap;
        this.classId2CodecMap = classId2CodecMap;
    }

    @Override
    public int getProviderId() {
        return CodecProviderConst.APP_PROVIDER_ID;
    }

    @Nullable
    @Override
    public Codec<?> getCodec(int classId) {
        return classId2CodecMap.get(classId);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public <T> Codec<T> getCodec(Class<T> clazz) {
        return (Codec<T>) type2CodecMap.get(clazz);
    }

    public static AppCodecProvider newInstance(List<AppPojoCodec<?>> codecList) {
        final Map<Class<?>, AppPojoCodec<?>> type2CodecMap = new IdentityHashMap<>(codecList.size());
        final Int2ObjectMap<AppPojoCodec<?>> classId2CodecMap = new Int2ObjectOpenHashMap<>(codecList.size(), Hash.FAST_LOAD_FACTOR);

        for (AppPojoCodec<?> codec : codecList) {
            type2CodecMap.put(codec.getEncoderClass(), codec);
            classId2CodecMap.put(codec.getClassId(), codec);
        }
        return new AppCodecProvider(type2CodecMap, classId2CodecMap);
    }
}
