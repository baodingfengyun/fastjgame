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

package com.wjybxx.fastjgame.net.type;

import com.wjybxx.fastjgame.util.dsl.ValueObject;
import org.checkerframework.checker.nullness.qual.NonNull;

import javax.annotation.Nonnull;

/**
 * 类型信息，一个{@link TypeModel}对应一个POJO类型{@link Class}。
 * <p>
 * Q：{@link #typeName()}字符串标识的作用？
 * A：1. 持久化时必须使用字符串标识，因为字符串标识相对于数字型标识，更容易保持稳定(不变)(用户需要管理这些标识)。
 * 2. 字符串标识相对于数字型标识，可以包含更多的信息，且其易读。
 * <p>
 * Q: {@link #typeId()}数字标识的作用？
 * A: 1. 序列化时可以使用数字标识，可以减少数据传输量，提高通信效率。
 * 2. {@link String#hashCode()}和{@link String#equals(Object)}效率较差，导致查找效率较低，而使用基本类型的long则快得多。
 * 3. 不可以使用数字标识进行持久化，因为要保证一个数值型标识稳定十分困难。
 * <p>
 * Q: 为什么要考虑{@link String#hashCode()}的性能？
 * A: 在网络传输或从持久化层读取时，总是新的字符串，因此每次都需要计算hash。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/4/20
 */
public final class TypeModel implements ValueObject {

    private final Class<?> type;
    private final String name;
    private final TypeId typeId;

    public TypeModel(Class<?> type, String name, TypeId typeId) {
        this.type = type;
        this.name = name;
        this.typeId = typeId;
    }

    /**
     * 获取对应的类型
     */
    @Nonnull
    public Class<?> type() {
        return type;
    }

    /**
     * 返回类型的字符串标识。
     */
    @Nonnull
    public String typeName() {
        return name;
    }

    /**
     * 返回类型的数字标识。
     */
    @NonNull
    public TypeId typeId() {
        return typeId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final TypeModel that = (TypeModel) o;
        return type == that.type
                && typeId == that.typeId
                && name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return 31 * type.hashCode()
                + 31 * name.hashCode()
                + typeId.hashCode();
    }

}
