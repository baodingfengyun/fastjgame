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

import java.util.List;

import static java.lang.String.format;

/**
 * 其实用户使用的类的概率是不太均匀的，由于原始类型和String和容器等类型生成代码都是直接调用的，因此进入这里的最大可能性是自定义对象。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/24
 */
public class CodecRegistryImp implements CodecRegistry {

    private final CodecProvider[] codecProviders;
    /**
     * provider数量较少，使用数组性能足够好，无需map
     */
    private final PojoCodecProvider[] pojoCodecProviders;

    CodecRegistryImp(List<? extends CodecProvider> codecProviders) {
        this.codecProviders = codecProviders.toArray(CodecProvider[]::new);
        this.pojoCodecProviders = codecProviders.stream()
                .filter(PojoCodecProvider.class::isInstance)
                .map(e -> (PojoCodecProvider) e)
                .toArray(PojoCodecProvider[]::new);
    }

    @Override
    public <T> Codec<? super T> get(Class<T> clazz) {
        for (CodecProvider codecProvider : codecProviders) {
            final Codec<T> codec = codecProvider.getCodec(clazz);
            if (codec != null) {
                return codec;
            }
        }
        throw new CodecConfigurationException(format("Can't find a codec for %s.", clazz));
    }

    @Override
    public Codec<?> getPojoCodec(int providerId, int classId) {
        for (PojoCodecProvider pojoCodecProvider : pojoCodecProviders) {
            if (pojoCodecProvider.getProviderId() == providerId) {
                return getCheckedCodec(pojoCodecProvider, classId);
            }
        }
        throw new CodecConfigurationException(format("Can't find a codec for %s:%s.", providerId, classId));
    }

    private static Codec<?> getCheckedCodec(final PojoCodecProvider codecProvider, int classId) {
        final Codec<?> codec = codecProvider.getCodec(classId);
        if (null == codec) {
            throw new CodecConfigurationException(format("Can't find a codec for %s:%s.", codecProvider.getProviderId(), classId));
        }
        return codec;
    }
}
