/*
 * Copyright 2019 wjybxx
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wjybxx.fastjgame.utils.concurrent;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * {@link BlockingFuture}的抽象实现
 *
 * @param <V>
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/14 14:53
 * github - https://github.com/hl845740757
 */
public abstract class AbstractBlockingFuture<V> implements BlockingFuture<V> {

    @Override
    public V get() throws InterruptedException, CompletionException {
        await();

        return getNow();
    }

    @Override
    public V get(long timeout, @Nonnull TimeUnit unit) throws InterruptedException, CompletionException, TimeoutException {
        if (await(timeout, unit)) {
            return getNow();
        } else {
            throw new TimeoutException();
        }
    }

    @Override
    public V join() throws CompletionException {
        awaitUninterruptibly();
        return getNow();
    }

    @Override
    public final boolean isVoid() {
        return false;
    }

}
