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

package com.wjybxx.fastjgame.net.common;

import com.wjybxx.fastjgame.utils.concurrent.EventLoop;
import com.wjybxx.fastjgame.utils.concurrent.FutureListener;
import com.wjybxx.fastjgame.utils.concurrent.timeout.TimeoutPromise;

import javax.annotation.Nonnull;

/**
 * RpcPromise
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/3
 * github - https://github.com/hl845740757
 */
public interface RpcPromise<V> extends RpcFuture<V>, TimeoutPromise<V> {

    @Override
    RpcPromise<V> await() throws InterruptedException;

    @Override
    RpcPromise<V> awaitUninterruptibly();

    @Override
    RpcPromise<V> addListener(@Nonnull FutureListener<? super V> listener, @Nonnull EventLoop bindExecutor);

    @Override
    RpcPromise<V> addListener(@Nonnull FutureListener<? super V> listener);

    @Override
    RpcPromise<V> removeListener(@Nonnull FutureListener<? super V> listener);

}
