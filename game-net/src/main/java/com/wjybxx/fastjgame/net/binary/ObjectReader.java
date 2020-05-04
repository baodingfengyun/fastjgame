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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.function.IntFunction;

/**
 * Pojo对象输入流
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/1/13
 * github - https://github.com/hl845740757
 */
public interface ObjectReader {

    int readInt() throws Exception;

    long readLong() throws Exception;

    float readFloat() throws Exception;

    double readDouble() throws Exception;

    short readShort() throws Exception;

    boolean readBoolean() throws Exception;

    byte readByte() throws Exception;

    char readChar() throws Exception;

    String readString() throws Exception;

    byte[] readBytes() throws Exception;

    /**
     * 从输入流中读取一个字段，如果没有对应的简便方法，可以使用该方法。
     */
    <T> T readObject() throws Exception;

    // ----------------------------------------- 处理多态问题 ----------------------------------

    /**
     * 从输入流中读取数据到collection中
     *
     * @param collectionFactory 创建集合的工厂 - 参数为元素个数，可能为0
     */
    @Nullable
    <C extends Collection<E>, E> C readCollection(@Nonnull IntFunction<C> collectionFactory) throws Exception;

    /**
     * 从输入流中读取数据到map中
     *
     * @param mapFactory 创建map的工厂 - 参数为元素个数，可能为0
     */
    @Nullable
    <M extends Map<K, V>, K, V> M readMap(@Nonnull IntFunction<M> mapFactory) throws Exception;

    /**
     * 从输入流中读取数据到数组中
     *
     * @param componentType 数组元素类型，支持基本类型（因此未定义为泛型参数）
     */
    @Nullable
    <T> T readArray(@Nonnull Class<?> componentType) throws Exception;

    /**
     * 读取一个多态实体对象(读取超类数据赋予子类实例)
     *
     * @param entityFactory    真正的实体创建工厂
     * @param entitySuperClass 实体对象的指定超类型。
     *                         注意：该超类型必须对应一个{@link AbstractPojoCodecImpl}
     */
    @Nullable
    <E> E readEntity(EntityFactory<E> entityFactory, Class<? super E> entitySuperClass) throws Exception;

    // --------------------------------------- 处理提前反序列化问题 ----------------------------------

    /**
     * 读取一个需要提前反序列化的对象。
     * 如果读取到的是bytes，则会对读取到的bytes进行一次解码操作。
     * 主要目的：使得对象可以在中间节点以bytes形式传输，然后在真正的接收方反序列化。
     */
    <T> T readPreDeserializeObject() throws Exception;
}
