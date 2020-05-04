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

import javax.annotation.concurrent.ThreadSafe;
import java.util.function.IntFunction;

/**
 * 实体类序列化工具类，每一个{@link PojoCodecImpl}只负责一个固定类型的解析。
 * (生成的代码会实现该接口)
 * <br>---------------------------------如何扩展------------------------<br>
 * 1. 序列化实现会通过泛型参数获取负责序列化的类型，因此只要进行了实现，就可以被自动加入。
 * 2. 一般而言，建议使用注解{@link SerializableClass}，并遵循相关规范，由注解处理器生成的类负责解析，而不是手写实现{@link PojoCodecImpl}。
 * 一旦手写实现，必须持久的进行维护。
 *
 * <br>-------------------------------什么时候手写实现？-----------------------<br>
 * 1. 当一个对象存在大量的反射解析导致性能瓶颈时，可以考虑手动实现。
 * 2. 如果对象存在复杂的构造过程的时候，可以考虑手动实现。 (解析构造方法会增加双方的复杂度)
 *
 * <br>-------------------------------实现时要注意什么？----------------------<br>
 * 1. 必须保证线程安全，最好是无状态的。
 * 2. 最好实现为目标类的静态内部类，且最好是private级别，不要暴露给外层。
 * 3. 必须有一个无参构造方法(可以private)。
 * 4. 必须使用{@link ObjectReader#readMap(IntFunction)}{@link ObjectReader#readCollection(IntFunction)}
 * 去读取map和collection，否则可能由于多态问题赋值失败。
 *
 * <br>-------------------------------如何实现多态解析----------------------<br>
 * 举个栗子：child1 -> parent -> parent或child2
 * 1. 必须手写实现。
 * 2. 必须采用组合方式，将要多态处理的类作为成员字段。
 * 3. 使用特定方法进行读写
 * {@link ObjectReader#readEntity(EntityFactory, Class)}
 * {@link ObjectWriter#writeEntity(Object, Class)}
 * PS: 其实Map和Collection的处理就是例子。
 *
 * @param <T> 要序列化的bean的类型
 * @author wjybxx
 * @version 1.0
 * date - 2020/1/13
 * github - https://github.com/hl845740757
 */
@ThreadSafe
public interface PojoCodecImpl<T> {

    /**
     * 获取负责编解码的类对象
     */
    Class<T> getEncoderClass();

    /**
     * 从输入流中解析指定对象。
     * 它应该创建对象，并反序列化该类及其所有超类定义的所有要序列化的字段。
     */
    T readObject(ObjectReader reader) throws Exception;

    /**
     * 将对象写入输出流。
     * 将对象及其所有超类定义的所有要序列化的字段写入输出流。
     *
     * @param instance 支持子类型
     */
    void writeObject(T instance, ObjectWriter writer) throws Exception;

}
