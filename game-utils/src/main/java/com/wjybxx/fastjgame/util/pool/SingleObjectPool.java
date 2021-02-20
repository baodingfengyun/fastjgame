package com.wjybxx.fastjgame.util.pool;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 相比与使用特定对象作为缓存，使用该缓存池可避免递归调用带来的bug。
 */
@NotThreadSafe
public class SingleObjectPool<T> implements ObjectPool<T> {

    private final Supplier<T> factory;
    private final ResetPolicy<T> resetPolicy;
    private T value;

    public SingleObjectPool(Supplier<T> factory, ResetPolicy<T> resetPolicy) {
        this.factory = Objects.requireNonNull(factory, "factory");
        this.resetPolicy = Objects.requireNonNull(resetPolicy, "resetPolicy");
    }

    @Override
    public T get() {
        T result = this.value;
        if (result != null) {
            this.value = null;
        } else {
            result = factory.get();
        }
        return result;
    }

    @Override
    public void returnOne(T object) {
        if (object == null) {
            throw new IllegalArgumentException("object cannot be null.");
        }
        resetPolicy.reset(object);
        this.value = object;
    }

    @Override
    public void returnAll(Collection<? extends T> objects) {
        if (objects == null) {
            throw new IllegalArgumentException("objects cannot be null.");
        }

        for (T v : objects) {
            if (null == v) {
                continue;
            }
            resetPolicy.reset(v);
            this.value = v;
        }
    }

    @Override
    public void returnAll(ArrayList<? extends T> objects) {
        if (objects == null) {
            throw new IllegalArgumentException("objects cannot be null.");
        }

        for (int i = 0, objectsSize = objects.size(); i < objectsSize; i++) {
            T v = objects.get(i);
            if (null == v) {
                continue;
            }
            resetPolicy.reset(v);
            this.value = v;
        }
    }

    @Override
    public int maxCount() {
        return 1;
    }

    @Override
    public int idleCount() {
        return value == null ? 0 : 1;
    }

    @Override
    public void clear() {
        value = null;
    }

}
