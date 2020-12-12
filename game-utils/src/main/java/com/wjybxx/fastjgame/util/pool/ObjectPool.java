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

package com.wjybxx.fastjgame.util.pool;


import java.util.Collection;
import java.util.function.Supplier;

/**
 * 对象池
 *
 * @param <T>
 */
public interface ObjectPool<T> extends Supplier<T> {

    /**
     * @return 如果池中有可用的对象，则返回缓存的对象，否则返回一个新的对象
     */
    @Override
    T get();

    /**
     * 将指定的对象放入池中，使其符合{@link #get()}返回的条件。
     * 如果池中已包含{@link #maxCount()}数量的空闲对象，指定的对象将会被重置，但不会被添加到的池中。
     * <p>
     * 如果{@code object}实现了{@link PoolableObject}接口，那么它的{@link PoolableObject#resetPoolable()}方法将被调用。
     *
     * @param object 要回收的对象
     */
    void returnOne(T object);

    /**
     * 将指定的对象放入池中，集合中的空对象被静默忽略。
     * 注意：该方法并不会调用集合的{@code clear}方法。
     * <p>
     * Q: 为什么不调用集合的{@code clear}方法？
     * A: 因为不能保证调用成功，可能就是个不可变集合。
     *
     * @param objects 要回收的对象
     */
    void returnAll(Collection<? extends T> objects);

    /**
     * @return 缓存池缓存对象数量上限
     */
    int maxCount();

    /**
     * @return 返回当前池中的对象数
     */
    int freeCount();

    /**
     * 删除此池中的所有可用对象
     */
    void clear();

}