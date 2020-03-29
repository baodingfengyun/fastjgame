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

import javax.annotation.Nonnull;
import java.util.concurrent.Executor;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/3/29
 */
public interface ListenablePromise<V> extends Promise<V>, ListenableFuture<V> {

    // 语法支持
    @Override
    ListenablePromise<V> addListener(@Nonnull FutureListener<? super V> listener);

    @Override
    ListenablePromise<V> addListener(@Nonnull FutureListener<? super V> listener, @Nonnull Executor bindExecutor);
}
