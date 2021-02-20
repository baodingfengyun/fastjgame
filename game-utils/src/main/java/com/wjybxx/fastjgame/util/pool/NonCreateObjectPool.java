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

import java.util.Objects;

/**
 * 一个只缓存对象，不创建对象的池
 *
 * @param <T>
 */
public class NonCreateObjectPool<T> extends AbstractObjectPool<T> {

    private final ResetPolicy<T> resetPolicy;

    public NonCreateObjectPool(ResetPolicy<T> resetPolicy) {
        this.resetPolicy = Objects.requireNonNull(resetPolicy, "resetPolicy");
    }

    public NonCreateObjectPool(ResetPolicy<T> resetPolicy, int initialCapacity) {
        super(initialCapacity);
        this.resetPolicy = Objects.requireNonNull(resetPolicy, "resetPolicy");
    }

    public NonCreateObjectPool(ResetPolicy<T> resetPolicy, int initialCapacity, int maxCapacity) {
        super(initialCapacity, maxCapacity);
        this.resetPolicy = Objects.requireNonNull(resetPolicy, "resetPolicy");
    }

    @Override
    protected T newObject() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void reset(T object) {
        resetPolicy.reset(object);
    }
}
