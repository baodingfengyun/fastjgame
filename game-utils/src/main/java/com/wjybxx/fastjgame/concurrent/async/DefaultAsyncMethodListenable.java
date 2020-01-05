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

package com.wjybxx.fastjgame.concurrent.async;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * 默认的监听管理器，在监听到结果之后将结果转发到真正的监听器上。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/1/5 21:21
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public class DefaultAsyncMethodListenable<V> implements AsyncMethodListenable<V>, AsyncMethodCallback<V> {

    private AsyncMethodResult<? extends V> asyncMethodResult;
    private AsyncMethodCallback<? super V> callback;

    @Override
    public void onComplete(@Nullable AsyncMethodResult<? extends V> asyncMethodResult) {
        assert this.asyncMethodResult == null;
        this.asyncMethodResult = asyncMethodResult;

        if (callback != null) {
            try {
                callback.onComplete(asyncMethodResult);
            } finally {
                callback = null;
            }
        }
    }

    @Override
    public AsyncMethodListenable<V> onSuccess(SucceededAsyncMethodCallback<? super V> callback) {
        addCallback(callback);
        return this;
    }

    @Override
    public AsyncMethodListenable<V> onFailure(FailedAsyncMethodCallback<? super V> callback) {
        addCallback(callback);
        return this;
    }

    @Override
    public AsyncMethodListenable<V> onComplete(AsyncMethodCallback<? super V> callback) {
        addCallback(callback);
        return this;
    }

    @SuppressWarnings("unchecked")
    private void addCallback(final AsyncMethodCallback<? super V> newCallback) {
        if (asyncMethodResult != null) {
            // 已执行完毕
            newCallback.onComplete(asyncMethodResult);
            return;
        }

        // 多数情况下我们都只有一个回调
        if (callback == null) {
            callback = newCallback;
            return;
        }
        // 添加超过两次
        if (callback instanceof CompositeAsyncMethodCallback) {
            ((CompositeAsyncMethodCallback<V>) this.callback).addChild(newCallback);
        } else {
            // 添加的第二个回调
            callback = new CompositeAsyncMethodCallback<>(callback, newCallback);
        }
    }
}
