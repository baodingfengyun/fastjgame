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
import java.util.concurrent.Executor;

/**
 * promise用于为关联的{@link BlockingFuture}赋值结果。
 *
 * @param <V>
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/14
 * github - https://github.com/hl845740757
 */
public interface BlockingPromise<V> extends Promise<V>, BlockingFuture<V> {

    // 仅用于语法支持
    @Override
    BlockingPromise<V> await() throws InterruptedException;

    @Override
    BlockingPromise<V> awaitUninterruptibly();

    @Override
    BlockingPromise<V> onComplete(@Nonnull FutureListener<? super V> listener);

    @Override
    BlockingPromise<V> onComplete(@Nonnull FutureListener<? super V> listener, @Nonnull Executor bindExecutor);

    @Override
    BlockingPromise<V> onSuccess(@Nonnull SucceededFutureListener<? super V> listener);

    @Override
    BlockingPromise<V> onSuccess(@Nonnull SucceededFutureListener<? super V> listener, @Nonnull Executor bindExecutor);

    @Override
    BlockingPromise<V> onFailure(@Nonnull FailedFutureListener<? super V> listener);

    @Override
    BlockingPromise<V> onFailure(@Nonnull FailedFutureListener<? super V> listener, @Nonnull Executor bindExecutor);

}
