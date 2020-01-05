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

import javax.annotation.concurrent.NotThreadSafe;

/**
 * 异步方法的监听管理器
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/1/5 21:18
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public interface AsyncMethodListenable<V> {

    /**
     * 设置方法成功时执行的回调。
     *
     * @param callback 回调逻辑
     * @return this
     */
    AsyncMethodListenable<V> onSuccess(SucceededAsyncMethodCallback<? super V> callback);

    /**
     * 设置方法失败时执行的回调。
     *
     * @param callback 回调逻辑
     * @return this
     */
    AsyncMethodListenable<V> onFailure(FailedAsyncMethodCallback<? super V> callback);

    /**
     * 设置无论成功还是失败都会执行的回调。
     *
     * @param callback 回调逻辑
     * @return this
     */
    AsyncMethodListenable<V> onComplete(AsyncMethodCallback<? super V> callback);

}
