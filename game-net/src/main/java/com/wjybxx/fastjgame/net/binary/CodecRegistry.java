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
 * 编解码器全局注册表，可以获取所有注册的编解码器
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/24
 */
public interface CodecRegistry {

    /**
     * 获取指定类class对应的编解码器
     *
     * @param clazz 注意：某些作为键的类可能不是实际被编解码的类，如：{@link java.util.Map}
     * @return codec
     * @throws CodecConfigurationException 如果不存在对应的编解码器，则抛出异常
     */
    <T> Codec<? extends T> get(Class<T> clazz);

    /**
     * 通过provider标识和class标识获取对应的codec
     *
     * @param providerId provider对应的id
     * @param classId    class在provider下对应的id
     * @return codec
     * @throws CodecConfigurationException 如果不存在对应的编解码器，则抛出异常
     */
    <T> Codec<?> get(int providerId, int classId);
}
