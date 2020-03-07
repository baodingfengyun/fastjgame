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

package com.wjybxx.fastjgame.utils.concurrent.timeout;

import com.wjybxx.fastjgame.utils.concurrent.*;

import javax.annotation.Nonnull;
import java.util.concurrent.Executor;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/3/7
 * github - https://github.com/hl845740757
 */
public class VoidTimeoutFuture extends VoidFuture implements TimeoutFuture<Object> {

    public VoidTimeoutFuture(EventLoop eventLoop) {
        super(eventLoop);
    }

    @Override
    public boolean isTimeout() {
        return false;
    }


    @Override
    public TimeoutFuture<Object> await() throws InterruptedException {
        super.await();
        return this;
    }

    @Override
    public TimeoutFuture<Object> awaitUninterruptibly() {
        super.awaitUninterruptibly();
        return this;
    }

    @Override
    public TimeoutFuture<Object> onComplete(@Nonnull FutureListener<? super Object> listener) {
        super.onComplete(listener);
        return this;
    }

    @Override
    public TimeoutFuture<Object> onComplete(@Nonnull FutureListener<? super Object> listener, @Nonnull Executor bindExecutor) {
        super.onComplete(listener, bindExecutor);
        return this;
    }

    @Override
    public TimeoutFuture<Object> onSuccess(@Nonnull SucceededFutureListener<? super Object> listener) {
        super.onSuccess(listener);
        return this;
    }

    @Override
    public TimeoutFuture<Object> onSuccess(@Nonnull SucceededFutureListener<? super Object> listener, @Nonnull Executor bindExecutor) {
        super.onSuccess(listener, bindExecutor);
        return this;
    }

    @Override
    public TimeoutFuture<Object> onFailure(@Nonnull FailedFutureListener<? super Object> listener) {
        super.onFailure(listener);
        return this;
    }

    @Override
    public TimeoutFuture<Object> onFailure(@Nonnull FailedFutureListener<? super Object> listener, @Nonnull Executor bindExecutor) {
        super.onFailure(listener, bindExecutor);
        return this;
    }

    @Override
    public TimeoutFuture<Object> onTimeout(@Nonnull TimeoutFutureListener<? super Object> listener, @Nonnull Executor bindExecutor) {
        super.onComplete(listener, bindExecutor);
        return this;
    }

    @Override
    public TimeoutFuture<Object> onTimeout(@Nonnull TimeoutFutureListener<? super Object> listener) {
        super.onComplete(listener);
        return this;
    }
}
