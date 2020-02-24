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

/**
 * 其实用户使用的类的概率是不太均匀的，由于原始类型和String和容器等类型生成代码都是直接调用的，因此进入这里的最大可能性是自定义对象。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/24
 */
public class CodecRegistryImp implements CodecRegistry {

    /**
     * 默认受支持的jdk对象编解码器提供者
     */
    private final CodecProvider jdkObjectCodecProvider;
    /**
     * 用户自定义对象编解码提供者
     */
    private final CodecProvider appObjectCodecProvider;
    /**
     * 容器对象编解码器提供者
     */
    private final CodecProvider containerObjectProvider;

    CodecRegistryImp(CodecProvider appObjectCodecProvider) {
        this.jdkObjectCodecProvider = JdkCodecProvider.INSTANCE;
        this.containerObjectProvider = ContainerCodecProvider.INSTANCE;
        this.appObjectCodecProvider = appObjectCodecProvider;
    }

    @Override
    public <T> Codec<? extends T> get(Class<T> clazz) {
        final Codec<T> appCodec = appObjectCodecProvider.getCodec(clazz);
        if (appCodec != null) {
            return appCodec;
        }

        final Codec<T> jdkCodec = jdkObjectCodecProvider.getCodec(clazz);
        if (jdkCodec != null) {
            return jdkCodec;
        }

        final Codec<T> containerCodec = containerObjectProvider.getCodec(clazz);
        if (null != containerCodec) {
            return containerCodec;
        }
        throw new CodecConfigurationException("Unsupported class " + clazz.getName());
    }

    @Override
    public Codec<?> get(int providerId, int classId) {
        if (providerId == appObjectCodecProvider.getProviderId()) {
            return getCheckedCodec(appObjectCodecProvider, classId);
        }

        if (providerId == jdkObjectCodecProvider.getProviderId()) {
            return getCheckedCodec(jdkObjectCodecProvider, classId);
        }

        if (providerId == containerObjectProvider.getProviderId()) {
            return getCheckedCodec(containerObjectProvider, classId);
        }

        throw new CodecConfigurationException("Unknown providerId " + providerId);
    }

    private static Codec<?> getCheckedCodec(final CodecProvider codecProvider, int classId) {
        final Codec<?> codec = codecProvider.getCodec(classId);
        if (null == codec) {
            throw new CodecConfigurationException("Unknown classId " + classId + ", provider is " + codecProvider.getProviderId());
        }
        return codec;
    }
}
