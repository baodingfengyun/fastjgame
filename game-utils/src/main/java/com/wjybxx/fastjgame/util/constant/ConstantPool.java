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

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
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
    private final FactoryWrapper<T> factory;

    public ConstantPool(ConstantFactory<T> factory) {
        this.factory = new FactoryWrapper<>(Objects.requireNonNull(factory, "factory"));
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
     * 一旦创建成功，则接下来的调用，则总是返回先前创建的常量。
     *
     * @param name 常量的名字
     */
    public final T valueOf(String name) {
        checkNotNullAndNotEmpty(name, "name");
        return getOrCreate(name);
    }

    /**
     * 创建一个常量，如果已存在关联的常量，则抛出异常。
     */
    public final T newInstance(String name) {
        checkNotNullAndNotEmpty(name, "name");
        return createOrThrow(name);
    }

    /**
     * @return 如果给定名字存在关联的常量，则返回true
     */
    public final boolean exists(String name) {
        checkNotNullAndNotEmpty(name, "name");
        return constants.containsKey(name);
    }

    /**
     * 获取一个常量，若不存在关联的常量则返回null。
     *
     * @return 返回常量名关联的常量，若不存在则返回null。
     */
    @Nullable
    public final T get(String name) {
        checkNotNullAndNotEmpty(name, "name");
        return constants.get(name);
    }

    /**
     * 获取一个常量，若不存在关联的常量则抛出异常
     *
     * @param name 常量的名字
     * @return 常量名关联的常量
     * @throws IllegalArgumentException 如果不存在对应的常量
     */
    public final T getOrThrow(String name) {
        checkNotNullAndNotEmpty(name, "name");
        final T constant = constants.get(name);
        if (null == constant) {
            throw new IllegalArgumentException(name + " does not exist");
        }
        return constant;
    }

    /**
     * 注意：该操作并不等同于枚举的{@code values()}，是个高开销操作；
     * 此外，如果存在竞态条件，那么每次返回的结果可能并不一致。
     *
     * @return 返回当前拥有的所有常量
     */
    public final List<T> values() {
        return new ArrayList<>(constants.values());
    }

    private static String checkNotNullAndNotEmpty(String value, String name) {
        Objects.requireNonNull(value, name);

        if (value.isEmpty()) {
            throw new IllegalArgumentException(name + " is empty ");
        }

        return value;
    }

    /**
     * 通过名字获取已存在的常量，或者当其不存在时创建新的常量。
     *
     * @param name 常量的名字
     */
    private T getOrCreate(String name) {
        T constant = constants.get(name);
        if (constant == null) {
            final T tempConstant = factory.apply(name);
            constant = constants.putIfAbsent(name, tempConstant);
            if (constant == null) {
                return tempConstant;
            }
        }

        return constant;
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

    private static class FactoryWrapper<T extends Constant<T>> implements Function<String, T> {

        private final AtomicInteger idGenerator = new AtomicInteger(1);
        private final ConstantFactory<T> delegate;

        FactoryWrapper(ConstantFactory<T> delegate) {
            this.delegate = delegate;
        }

        private int nextId() {
            return idGenerator.getAndIncrement();
        }

        @Override
        public T apply(String name) {
            final int id = nextId();
            final T result = delegate.newConstant(id, name);
            Objects.requireNonNull(result, "result");
            if (result.id() == id && Objects.equals(result.name(), name)) {
                // 校验实现
                return result;
            }
            final String msg = String.format("expected id: %d, name: %s, found id: %d, name: %s", id, name, result.id(), result.name());
            throw new BadImplementationException(msg);
        }
    }
}
