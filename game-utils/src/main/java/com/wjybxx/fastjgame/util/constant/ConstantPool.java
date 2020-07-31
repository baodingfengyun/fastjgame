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

package com.wjybxx.fastjgame.util.constant;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

/**
 * 常量池
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/7/31
 */
public final class ConstantPool<T extends Constant<T>> {

    private final ConcurrentMap<String, T> constants = new ConcurrentHashMap<>();

    private final Function<String, T> factory;

    public ConstantPool(Function<String, T> factory) {
        this.factory = Objects.requireNonNull(factory, "factory");
    }

    /**
     * Shortcut of {@link #valueOf(String) valueOf(firstNameComponent.getName() + "#" + secondNameComponent)}.
     *
     * @param firstNameComponent  充当命名空间
     * @param secondNameComponent 命名空间内的名字
     */
    public final T valueOf(Class<?> firstNameComponent, String secondNameComponent) {
        final String nameSpace = Objects.requireNonNull(firstNameComponent, "firstNameComponent").getName();
        final String name = checkNotNullAndNotEmpty(secondNameComponent, "secondNameComponent");
        return valueOf(nameSpace + "#" + name);
    }

    /**
     * 获取给定名字对应的常量。
     * 如果关联的常量上不存在，则创建一个新的常量并返回。
     * 一旦创建成功，则接下来的调用，则总是返回先前创建的常量，就好像一个单例。
     *
     * @param name 常量的名字
     */
    public final T valueOf(String name) {
        checkNotNullAndNotEmpty(name, "name");
        return getOrCreate(name);
    }

    /**
     * 通过名字获取已存在的常量，或者当其不存在时创建新的常量。
     *
     * @param name 常量的名字
     */
    private T getOrCreate(String name) {
        return constants.computeIfAbsent(name, factory);
    }

    /**
     * 创建一个常量，如果已存在关联的常量，则抛出异常。
     */
    public final T newInstance(String name) {
        checkNotNullAndNotEmpty(name, "name");
        return createOrThrow(name);
    }

    /**
     * 创建一个常量，或者已存在关联的常量时则抛出异常
     *
     * @param name 常量的名字
     */
    private T createOrThrow(String name) {
        T constant = constants.get(name);

        if (constant == null) {
            final T tempConstant = factory.apply(name);
            constant = constants.putIfAbsent(name, tempConstant);
            if (constant == null) {
                return tempConstant;
            }
        }

        throw new IllegalArgumentException(name + " is already in use");
    }

    /**
     * @return 如果给定名字存在关联的常量，则返回true
     */
    public boolean exists(String name) {
        checkNotNullAndNotEmpty(name, "name");
        return constants.containsKey(name);
    }

    private static String checkNotNullAndNotEmpty(String value, String valueName) {
        Objects.requireNonNull(value, valueName);

        if (value.isEmpty()) {
            throw new IllegalArgumentException("empty " + valueName);
        }

        return value;
    }

}
