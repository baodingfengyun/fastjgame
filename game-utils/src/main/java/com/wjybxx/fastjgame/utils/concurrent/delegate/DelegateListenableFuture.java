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

import com.wjybxx.fastjgame.utils.concurrent.EventLoop;
import com.wjybxx.fastjgame.utils.concurrent.FutureListener;
import com.wjybxx.fastjgame.utils.concurrent.ListenableFuture;

import javax.annotation.Nonnull;
import java.util.concurrent.Executor;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/3/10
 */
public class DelegateListenableFuture<V> extends DelegateFuture<V> implements ListenableFuture<V> {

    public DelegateListenableFuture(ListenableFuture<V> delegate) {
        super(delegate);
    }

    @Override
    protected ListenableFuture<V> getDelegate() {
        return (ListenableFuture<V>) super.getDelegate();
    }

    @Override
    public final EventLoop defaultExecutor() {
        return getDelegate().defaultExecutor();
    }

    /**
     * @return 必须返回this，声明为{@link DelegateListenableFuture}可以避免错误的返回值
     */
    @Override
    public DelegateListenableFuture<V> addListener(@Nonnull FutureListener<? super V> listener) {
        getDelegate().addListener(listener);
        return this;
    }

    @Override
    public DelegateListenableFuture<V> addListener(@Nonnull FutureListener<? super V> listener, @Nonnull Executor bindExecutor) {
        getDelegate().addListener(listener, bindExecutor);
        return this;
    }

}
