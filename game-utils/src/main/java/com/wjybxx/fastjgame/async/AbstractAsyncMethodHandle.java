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

package com.wjybxx.fastjgame.async;

import com.wjybxx.fastjgame.concurrent.*;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * AsyncMethodHandle模板实现，实现listener管理
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/1/8
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public abstract class AbstractAsyncMethodHandle<T, FR extends FutureResult<V>, V> implements AsyncMethodHandle<T, FR, V> {

    private GenericFutureResultListener<FR> listener;

    @Override
    public AsyncMethodHandle<T, FR, V> onSuccess(GenericFutureSuccessResultListener<FR, V> listener) {
        addListener(listener);
        return this;
    }

    @Override
    public AsyncMethodHandle<T, FR, V> onFailure(GenericFutureFailureResultListener<FR> listener) {
        addListener(listener);
        return this;
    }

    @Override
    public AsyncMethodHandle<T, FR, V> onComplete(GenericFutureResultListener<FR> listener) {
        addListener(listener);
        return this;
    }

    protected final void addListener(GenericFutureResultListener<FR> child) {
        if (this.listener == null) {
            this.listener = child;
            return;
        }
        if (this.listener instanceof FutureResultListenerContainer) {
            ((FutureResultListenerContainer<FR>) this.listener).addChild(child);
        } else {
            this.listener = new FutureResultListenerContainer<>(this.listener, child);
        }
    }

    protected final GenericFutureResultListener<FR> detachListener() {
        GenericFutureResultListener<FR> result = this.listener;
        this.listener = null;
        return result;
    }
}
