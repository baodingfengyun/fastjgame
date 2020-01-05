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

import javax.annotation.Nonnull;

/**
 * 执行失败的异步方法的回调，当执行成功时，不会执行{@link #onFailure(Throwable)}。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/1/5 18:59
 * github - https://github.com/hl845740757
 */
@FunctionalInterface
public interface FailedAsyncMethodCallback<V> extends AsyncMethodCallback<V> {

    @Override
    default void onComplete(AsyncMethodResult<? extends V> asyncMethodResult) {
        if (asyncMethodResult.isFailure()) {
            onFailure(asyncMethodResult.getCause());
        }
    }

    void onFailure(@Nonnull Throwable cause);
}
