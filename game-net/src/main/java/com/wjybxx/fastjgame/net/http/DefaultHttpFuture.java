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

import com.wjybxx.fastjgame.utils.concurrent.EventLoop;
import com.wjybxx.fastjgame.utils.concurrent.FutureListener;
import com.wjybxx.fastjgame.utils.concurrent.adapter.CompletableFutureAdapter;

import javax.annotation.Nonnull;
import java.net.http.HttpTimeoutException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/1/10
 * github - https://github.com/hl845740757
 */
public class DefaultHttpFuture<V> extends CompletableFutureAdapter<V> implements HttpFuture<V> {

    public DefaultHttpFuture(EventLoop executor, CompletableFuture<V> future) {
        super(executor, future);
    }

    @Override
    public boolean isTimeout() {
        return cause() instanceof HttpTimeoutException;
    }

    // 语法支持
    @Override
    public DefaultHttpFuture<V> addListener(@Nonnull FutureListener<? super V> listener) {
        super.addListener(listener);
        return this;
    }

    @Override
    public DefaultHttpFuture<V> addListener(@Nonnull FutureListener<? super V> listener, @Nonnull Executor bindExecutor) {
        super.addListener(listener, bindExecutor);
        return this;
    }

}
