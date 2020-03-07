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

package com.wjybxx.fastjgame.utils.concurrent;

import java.util.ArrayList;
import java.util.List;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/3/6
 */
public class CompositeFutureListener<V> implements FutureListener<V> {

    private static final int DEFAULT_INIT_CAPACITY = 4;

    private final List<FutureListener<? super V>> children;

    public CompositeFutureListener(FutureListener<? super V> first, FutureListener<? super V> second) {
        this(DEFAULT_INIT_CAPACITY, first, second);
    }

    public CompositeFutureListener(int initCapacity, FutureListener<? super V> first, FutureListener<? super V> second) {
        children = new ArrayList<>(initCapacity);
        children.add(first);
        children.add(second);
    }

    public void addChild(FutureListener<? super V> child) {
        children.add(child);
    }

    @Override
    public void onComplete(NListenableFuture<V> future) throws Exception {
        for (FutureListener<? super V> futureListener : children) {
            DefaultPromise.notifyListenerNowSafely(future, futureListener);
        }
    }
}
