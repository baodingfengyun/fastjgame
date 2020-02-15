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

package com.wjybxx.fastjgame.net.http;

import com.wjybxx.fastjgame.utils.annotation.UnstableApi;
import com.wjybxx.fastjgame.utils.concurrent.EventLoop;
import com.wjybxx.fastjgame.utils.concurrent.FutureListener;
import com.wjybxx.fastjgame.utils.concurrent.timeout.TimeoutFuture;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/1/10
 * github - https://github.com/hl845740757
 */
public interface HttpFuture<V> extends TimeoutFuture<V> {

    /**
     * 当cause为{@link java.net.http.HttpTimeoutException}时表示超时
     */
    boolean isTimeout();

    @UnstableApi
    @Nullable
    @Override
    HttpFutureResult<V> getAsResult();

    @Override
    HttpFuture<V> await() throws InterruptedException;

    @Override
    HttpFuture<V> awaitUninterruptibly();

    @Override
    HttpFuture<V> addListener(@Nonnull FutureListener<? super V> listener);

    @Override
    HttpFuture<V> addListener(@Nonnull FutureListener<? super V> listener, @Nonnull EventLoop bindExecutor);

    @Override
    HttpFuture<V> removeListener(@Nonnull FutureListener<? super V> listener);
}
