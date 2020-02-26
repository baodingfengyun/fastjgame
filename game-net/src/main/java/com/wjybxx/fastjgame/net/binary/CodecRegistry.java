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

import java.util.Collection;
import java.util.Map;

/**
 * {@link Codec}全局注册表，可以获取所有注册的{@link Codec}。
 * 1. 它是网络层与{@link Codec}交互的中介，也是{@link Codec}与{@link Codec}交互的中介。
 * 2. 它由一组{@link CodecProvider}组成，而{@link CodecProvider}由{@link Codec}组成。
 * <br>-------------------------------------------------<br>
 * 题外话，吐槽一下MongoDB的{@link CodecRegistry}的循环依赖问题，不知道大家有没有被恶心过，反正我是被恶心过。
 * 它设计最致命的一点就是编解码接口{@link Codec}中的编解码方法中没有{@link CodecRegistry}参数，
 * 因此：如果一个codec中需要编解码其它类型对象的时候，必须持有需要的所有的{@link Codec}或{@link CodecRegistry}对象，
 * 而构造{@link CodecRegistry}对象又需要所有的codec，于是，你不能直接创建{@link Codec}，
 * 你需要引入他设计的{@link CodecProvider}，进行延迟创建，在实现的时候判断class信息，并创建对应的codec。
 * 问题是它的{@link CodecProvider}概念又不够清晰，为什么有{@link CodecRegistry}参数，这个{@link CodecRegistry}是谁？？？
 * <p>
 * 总之一堆卧槽，他们需要解决循环依赖问题，你也需要解决循环依赖问题，结果就引入了更多的概念，而这些概念又造成了更多的问题：
 * 理解复杂，使用复杂，性能也有损。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/24
 */
public interface CodecRegistry {

    /**
     * 获取指定类class对应的编解码器
     *
     * @throws CodecConfigurationException 如果不存在对应的编解码器，则抛出异常
     */
    <T> Codec<T> get(Class<T> clazz);

    /**
     * 通过provider标识和class标识获取对应的{@link PojoCodec}
     *
     * @param providerId provider对应的id
     * @param classId    class在provider下对应的id
     * @return codec
     * @throws CodecConfigurationException 如果不存在对应的编解码器，则抛出异常
     */
    PojoCodec<?> getPojoCodec(int providerId, int classId);

    // 减少查询
    Codec<Object> getArrayCodec();

    Codec<Map<?, ?>> getMapCodec();

    Codec<Collection<?>> getCollectionCodec();

}
