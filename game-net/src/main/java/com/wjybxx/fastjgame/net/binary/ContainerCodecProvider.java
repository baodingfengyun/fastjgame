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

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/24
 */
public class ContainerCodecProvider implements CodecProvider {

    public static final ContainerCodecProvider INSTANCE = new ContainerCodecProvider();

    private static final int COLLECTION_CLASS_ID = 1;
    private static final int MAP_CLASS_ID = 2;
    private static final int ARRAY_CLASS_ID = 3;

    private final CollectionCodec collectionCodec = new CollectionCodec(COLLECTION_CLASS_ID);
    private final MapCodec mapCodec = new MapCodec(MAP_CLASS_ID);
    private final ArrayCodec arrayCodec = new ArrayCodec(ARRAY_CLASS_ID);

    private ContainerCodecProvider() {
    }

    @Override
    public int getProviderId() {
        return CodecProviderConst.CONTAINER_PROVIDER_ID;
    }

    @Nullable
    @Override
    public Codec<?> getCodec(int classId) {
        if (classId == COLLECTION_CLASS_ID) {
            return collectionCodec;
        }
        if (classId == MAP_CLASS_ID) {
            return mapCodec;
        }
        if (classId == ARRAY_CLASS_ID) {
            return arrayCodec;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public <T> Codec<T> getCodec(Class<T> clazz) {
        if (Collection.class.isAssignableFrom(clazz)) {
            return (Codec<T>) collectionCodec;
        }

        if (Map.class.isAssignableFrom(clazz)) {
            return (Codec<T>) mapCodec;
        }
        if (clazz.isArray() || clazz == ArrayCodec.ARRAY_CLASS_KEY) {
            return (Codec<T>) arrayCodec;
        }
        return null;
    }
}
