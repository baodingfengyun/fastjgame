package com.wjybxx.fastjgame.util.pool;

import java.util.Collection;
import java.util.Map;

/**
 * 对象池对象的重置策略。
 * <p>
 * Q: 该接口的意义？
 * A: 1. 使用组合的方式更加灵活，尤其是遇到一些非自己定义的类对象时。
 * 2. 可避免外部忘记重置对象。以前定义的{@code returnNotReset}极其危险，因此删除了。
 */
@FunctionalInterface
public interface ResetPolicy<V> {

    void reset(V object);

    /**
     * 如果{@code object}实现了{@link PoolableObject}接口，那么它的{@link PoolableObject#resetPoolable()}方法将被调用。
     */
    static <V> ResetPolicy<V> resetIfPoolableObject() {
        return object -> {
            if (object instanceof PoolableObject) {
                ((PoolableObject) object).resetPoolable();
            }
        };
    }

    static <V extends Collection<?>> ResetPolicy<V> clearCollection() {
        return object -> object.clear();
    }

    static <V extends Map<?, ?>> ResetPolicy<V> clearMap() {
        return object -> object.clear();
    }

    /**
     * 对象还入池中时什么也不做。
     */
    static <V> ResetPolicy<V> notReset() {
        return object -> {
        };
    }

}