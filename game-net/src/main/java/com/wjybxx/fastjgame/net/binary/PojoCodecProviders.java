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

import com.wjybxx.fastjgame.net.binaryextend.ClassCodec;
import com.wjybxx.fastjgame.utils.misc.IntPair;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/25
 */
public class PojoCodecProviders {

    /**
     * 内部使用的providerId区间
     * 自定义provider应该在这之外，并且大于0，小于127
     */
    public static final IntPair INTERNAL_PROVIDER_ID_RANGE = new IntPair(1, 10);

    private static final List<PojoCodecProvider> JDK_POJO_CODEC_PROVIDER = fromCodecsInternal(Arrays.asList(
            new ClassCodec((byte) 1, 1)
    ));

    /**
     * 获取系统默认支持的provider
     */
    public static List<CodecProvider> getDefaultProviders() {
        final List<CodecProvider> result = new ArrayList<>(JDK_POJO_CODEC_PROVIDER.size() + 2);
        result.add(new ValueCodecProvider());
        result.addAll(JDK_POJO_CODEC_PROVIDER);
        result.add(new ContainerCodecProvider());
        return result;
    }

    public static List<PojoCodecProvider> fromCodecs(List<? extends PojoCodec<?>> pojoCodecs) {
        checkProviderId(pojoCodecs);
        return fromCodecsInternal(pojoCodecs);
    }

    private static void checkProviderId(List<? extends PojoCodec<?>> pojoCodecs) {
        for (PojoCodec<?> pojoCodec : pojoCodecs) {
            checkMin(pojoCodec);
        }
    }

    private static void checkMin(PojoCodec<?> pojoCodec) {
        if (pojoCodec.getProviderId() <= INTERNAL_PROVIDER_ID_RANGE.getFirst()) {
            throw new IllegalArgumentException(String.format("%s's providerId %s must greater than %s",
                    pojoCodec.getEncoderClass().getSimpleName(),
                    pojoCodec.getProviderId(),
                    INTERNAL_PROVIDER_ID_RANGE.getFirst()
            ));
        }
    }

    private static List<PojoCodecProvider> fromCodecsInternal(List<? extends PojoCodec<?>> pojoCodecs) {
        final List<PojoCodecProvider> result = new ArrayList<>();
        pojoCodecs.stream()
                .collect(Collectors.groupingBy(PojoCodec::getProviderId))
                .forEach((providerId, codecLiset) -> {
                    result.add(newPojoCodecProvider(providerId, codecLiset));
                });
        return result;
    }

    private static PojoCodecProvider newPojoCodecProvider(byte providerId, List<? extends PojoCodec<?>> codecLiset) {
        final Map<Class<?>, PojoCodec<?>> type2CodecMap = new IdentityHashMap<>(codecLiset.size());
        final Int2ObjectMap<PojoCodec<?>> classId2CodecMap = new Int2ObjectOpenHashMap<>(codecLiset.size(), Hash.FAST_LOAD_FACTOR);
        for (PojoCodec<?> pojoCodec : codecLiset) {

            if (type2CodecMap.containsKey(pojoCodec.getEncoderClass())) {
                throw new IllegalArgumentException(String.format("%s has more than one codec",
                        pojoCodec.getEncoderClass().getName()));
            }

            if (classId2CodecMap.containsKey(pojoCodec.getClassId())) {
                throw new IllegalArgumentException(String.format("%s's classId is equals to %s ",
                        pojoCodec.getEncoderClass().getName(),
                        classId2CodecMap.get(pojoCodec.getClassId()).getEncoderClass().getName()));
            }

            type2CodecMap.put(pojoCodec.getEncoderClass(), pojoCodec);
            classId2CodecMap.put(pojoCodec.getClassId(), pojoCodec);
        }
        return new DefaultPojoCodecProvider(providerId, type2CodecMap, classId2CodecMap);
    }

    private static class DefaultPojoCodecProvider implements PojoCodecProvider {

        private final byte providerId;
        private final Map<Class<?>, PojoCodec<?>> type2CodecMap;
        private final Int2ObjectMap<PojoCodec<?>> classId2CodecMap;

        private DefaultPojoCodecProvider(byte providerId,
                                         Map<Class<?>, PojoCodec<?>> type2CodecMap,
                                         Int2ObjectMap<PojoCodec<?>> classId2CodecMap) {
            this.providerId = providerId;
            this.type2CodecMap = type2CodecMap;
            this.classId2CodecMap = classId2CodecMap;
        }

        @Override
        public byte getProviderId() {
            return providerId;
        }

        @Nullable
        @Override
        public PojoCodec<?> getPojoCodec(int classId) {
            return classId2CodecMap.get(classId);
        }

        @SuppressWarnings("unchecked")
        @Nullable
        @Override
        public <T> Codec<T> getCodec(Class<T> clazz) {
            return (Codec<T>) type2CodecMap.get(clazz);
        }
    }
}
