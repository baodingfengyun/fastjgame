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

import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/25
 */
public class CodecRegistrys {

    /**
     * @param appPojoCodecList 用户自定义的编解码器集合
     */
    public static CodecRegistry fromAppPojoCodecs(List<? extends PojoCodec<?>> appPojoCodecList) {
        // 用户使用自定义的pojo的概率远大于底层支持的pojo，因此要把用户的provider放在默认的provider的前面，加快搜索效率
        final List<CodecProvider> pojoCodecProviders = new ArrayList<>(PojoCodecProviders.fromCodecs(appPojoCodecList));
        pojoCodecProviders.addAll(PojoCodecProviders.getDefaultProviders());
        return new CodecRegistryImp(pojoCodecProviders);
    }

    private static class CodecRegistryImp implements CodecRegistry {

        private final CodecProvider[] codecProviders;
        private final PojoCodecProvider[] pojoCodecProviders;
        private final ContainerCodecProvider containerCodecProvider;

        CodecRegistryImp(List<? extends CodecProvider> codecProviders) {
            this.codecProviders = codecProviders.toArray(CodecProvider[]::new);
            this.pojoCodecProviders = codecProviders.stream()
                    .filter(PojoCodecProvider.class::isInstance)
                    .map(PojoCodecProvider.class::cast)
                    .toArray(PojoCodecProvider[]::new);

            this.containerCodecProvider = codecProviders.stream()
                    .filter(ContainerCodecProvider.class::isInstance)
                    .findFirst()
                    .map(ContainerCodecProvider.class::cast)
                    .get();
        }

        @Override
        public <T> Codec<T> get(Class<T> clazz) {
            for (CodecProvider codecProvider : codecProviders) {
                final Codec<T> codec = codecProvider.getCodec(clazz);
                if (codec != null) {
                    return codec;
                }
            }
            throw new CodecConfigurationException(format("Can't find a codec for %s.", clazz));
        }

        @Override
        public PojoCodec<?> getPojoCodec(int providerId, int classId) {
            // 理论上provider数量较少，使用数组性能足够好，无需map
            for (PojoCodecProvider pojoCodecProvider : pojoCodecProviders) {
                if (pojoCodecProvider.getProviderId() == providerId) {
                    return getCheckedCodec(pojoCodecProvider, classId);
                }
            }
            throw new CodecConfigurationException(format("Can't find a codec for %s:%s.", providerId, classId));
        }

        private static PojoCodec<?> getCheckedCodec(final PojoCodecProvider codecProvider, int classId) {
            final PojoCodec<?> codec = codecProvider.getPojoCodec(classId);
            if (null == codec) {
                throw new CodecConfigurationException(format("Can't find a codec for %s:%s.", codecProvider.getProviderId(), classId));
            }
            return codec;
        }

        @Override
        public ArrayCodec getArrayCodec() {
            return containerCodecProvider.getArrayCodec();
        }

        @Override
        public MapCodec getMapCodec() {
            return containerCodecProvider.getMapCodec();
        }

        @Override
        public CollectionCodec getCollectionCodec() {
            return containerCodecProvider.getCollectionCodec();
        }
    }
}
