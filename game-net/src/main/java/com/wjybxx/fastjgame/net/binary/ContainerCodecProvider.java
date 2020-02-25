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
 * 它对应于{@link Tag}中的3中容器类型
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/24
 */
public class ContainerCodecProvider implements CodecProvider {

    public static final ContainerCodecProvider INSTANCE = new ContainerCodecProvider();

    private final CollectionCodec collectionCodec = new CollectionCodec();
    private final MapCodec mapCodec = new MapCodec();
    private final ArrayCodec arrayCodec = new ArrayCodec();

    private ContainerCodecProvider() {
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

        if (clazz.isArray() || clazz == ArrayCodec.ARRAY_ENCODER_CLASS) {
            return (Codec<T>) arrayCodec;
        }
        return null;
    }
}
