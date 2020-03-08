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
 * 具有时效性的Promise
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/1/6
 * github - https://github.com/hl845740757
 */
public interface TimeoutPromise<V> extends NPromise<V>, TimeoutFuture<V> {

    // 仅用于语法支持
    @Override
    TimeoutPromise<V> onComplete(@Nonnull FutureListener<? super V> listener);

    @Override
    TimeoutPromise<V> onComplete(@Nonnull FutureListener<? super V> listener, @Nonnull Executor bindExecutor);

    @Override
    TimeoutPromise<V> onSuccess(@Nonnull SucceededFutureListener<? super V> listener);

    @Override
    TimeoutPromise<V> onSuccess(@Nonnull SucceededFutureListener<? super V> listener, @Nonnull Executor bindExecutor);

    @Override
    TimeoutPromise<V> onFailure(@Nonnull FailedFutureListener<? super V> listener);

    @Override
    TimeoutPromise<V> onFailure(@Nonnull FailedFutureListener<? super V> listener, @Nonnull Executor bindExecutor);

    @Override
    TimeoutPromise<V> onTimeout(@Nonnull TimeoutFutureListener<? super V> listener);

    @Override
    TimeoutPromise<V> onTimeout(@Nonnull TimeoutFutureListener<? super V> listener, @Nonnull Executor bindExecutor);
}
