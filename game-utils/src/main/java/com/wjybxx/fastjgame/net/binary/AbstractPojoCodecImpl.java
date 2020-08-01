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

import com.wjybxx.fastjgame.util.dsl.IndexableObject;

/**
 * {@link IndexableObject}之外的serializer会继承该类。
 * 实现该接口的类可以实现多态读写对象。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/18
 */
public abstract class AbstractPojoCodecImpl<T> implements PojoCodecImpl<T> {

    @Override
    public final T readObject(ObjectReader reader) throws Exception {
        final T instance = newInstance();
        readFields(instance, reader);
        return instance;
    }

    /**
     * 创建一个对象，如果是一个抽象类，应该抛出异常
     */
    protected abstract T newInstance() throws Exception;

    /**
     * 从输入流中读取所有序列化的字段到指定实例上。
     * (包括继承得到的字段)
     *
     * @param instance 支持子类型
     */
    public abstract void readFields(T instance, ObjectReader reader) throws Exception;
}
