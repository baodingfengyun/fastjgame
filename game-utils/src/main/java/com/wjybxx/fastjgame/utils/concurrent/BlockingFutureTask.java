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

import com.wjybxx.fastjgame.utils.concurrent.delegate.DelegateBlockingFuture;

import java.util.concurrent.Callable;
import java.util.concurrent.RunnableFuture;

/**
 * 与{@link java.util.concurrent.FutureTask}相似。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/14 20:32
 * github - https://github.com/hl845740757
 */
public class BlockingFutureTask<V> extends DelegateBlockingFuture<V> implements RunnableFuture<V> {

    private final Callable<V> callable;

    BlockingFutureTask(EventLoop executor, Callable<V> callable) {
        super(executor.newBlockingPromise());
        this.callable = callable;
    }

    @Override
    public void run() {
        final BlockingPromise<V> promise = (BlockingPromise<V>) getDelegate();
        try {
            if (promise.setUncancellable()) {
                V result = callable.call();
                promise.setSuccess(result);
            }
        } catch (Throwable e) {
            promise.setFailure(e);
        }
    }

}
