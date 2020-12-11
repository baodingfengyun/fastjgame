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

import javax.annotation.concurrent.NotThreadSafe;
import java.util.ArrayList;
import java.util.Collection;

/**
 * 对象池的默认实现
 *
 * @param <T> 池中对象类型
 */
@NotThreadSafe
public abstract class AbstractPool<T> implements Pool<T> {

    private final ArrayList<T> freeObjects;
    private final int maxCapacity;

    public AbstractPool() {
        this(16, Integer.MAX_VALUE);
    }

    public AbstractPool(int initialCapacity) {
        this(initialCapacity, Integer.MAX_VALUE);
    }

    public AbstractPool(int initialCapacity, int maxCapacity) {
        this.freeObjects = new ArrayList<>(initialCapacity);
        this.maxCapacity = maxCapacity;
    }

    protected abstract T newObject();

    @Override
    public T get() {
        // 从最后一个开始删除可避免复制
        return freeObjects.size() == 0 ? newObject() : freeObjects.remove(freeObjects.size() - 1);
    }

    @Override
    public int maxCount() {
        return maxCapacity;
    }

    @Override
    public int freeCount() {
        return freeObjects.size();
    }

    @Override
    public void free(T object) {
        if (object == null) {
            throw new IllegalArgumentException("object cannot be null.");
        }

        // 先调用reset，避免reset出现异常导致添加脏对象到缓存池中
        reset(object);

        if (freeObjects.size() < maxCapacity) {
            freeObjects.add(object);
        }
    }

    protected void reset(T object) {
        if (object instanceof Poolable) ((Poolable) object).reset();
    }

    @Override
    public void freeAll(Collection<? extends T> objects) {
        if (objects == null) {
            throw new IllegalArgumentException("objects cannot be null.");
        }

        final ArrayList<T> freeObjects = this.freeObjects;
        final int maxCapacity = this.maxCapacity;

        for (T e : objects) {
            if (null == e) {
                continue;
            }

            reset(e);

            if (freeObjects.size() < maxCapacity) {
                freeObjects.add(e);
            }
        }
    }

    @Override
    public void clear() {
        freeObjects.clear();
    }
}
