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

package com.wjybxx.fastjgame.utils.concurrent.delegate;

import com.wjybxx.fastjgame.utils.concurrent.BlockingFuture;
import com.wjybxx.fastjgame.utils.concurrent.FutureListener;

import javax.annotation.Nonnull;
import java.util.concurrent.*;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/3/10
 */
public class DelegateBlockingFuture<V> extends DelegateListenableFuture<V> implements BlockingFuture<V> {

    public DelegateBlockingFuture(BlockingFuture<V> delegate) {
        super(delegate);
    }

    @Override
    protected BlockingFuture<V> getDelegate() {
        return (BlockingFuture<V>) super.getDelegate();
    }

    @Override
    public final V get() throws InterruptedException, ExecutionException {
        return getDelegate().get();
    }

    @Override
    public final V get(long timeout, @Nonnull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return getDelegate().get(timeout, unit);
    }

    @Override
    public final V join() throws CompletionException {
        return getDelegate().join();
    }

    @Override
    public final boolean await(long timeout, @Nonnull TimeUnit unit) throws InterruptedException {
        return getDelegate().await(timeout, unit);
    }

    // 以下方法必须返回this
    @Override
    public BlockingFuture<V> await() throws InterruptedException {
        getDelegate().await();
        return this;
    }

    @Override
    public BlockingFuture<V> awaitUninterruptibly() {
        getDelegate().awaitUninterruptibly();
        return this;
    }

    @Override
    public boolean awaitUninterruptibly(long timeout, @Nonnull TimeUnit unit) {
        return getDelegate().awaitUninterruptibly(timeout, unit);
    }

    @Override
    public DelegateBlockingFuture<V> addListener(@Nonnull FutureListener<? super V> listener) {
        super.addListener(listener);
        return this;
    }

    @Override
    public DelegateBlockingFuture<V> addListener(@Nonnull FutureListener<? super V> listener, @Nonnull Executor bindExecutor) {
        super.addListener(listener, bindExecutor);
        return this;
    }

}
