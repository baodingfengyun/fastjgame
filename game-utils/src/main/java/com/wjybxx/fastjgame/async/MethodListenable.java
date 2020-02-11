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

package com.wjybxx.fastjgame.async;

import com.wjybxx.fastjgame.concurrent.FutureListener;
import com.wjybxx.fastjgame.concurrent.FutureResult;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * 方法结果监听管理器
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/6
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public interface MethodListenable<FR extends FutureResult<V>, V> extends FutureListener<V> {

    /**
     * 设置成功时执行的回调。
     *
     * @param listener 回调逻辑
     * @return this
     */
    MethodListenable<FR, V> onSuccess(GenericSuccessFutureResultListener<FR, V> listener);

    /**
     * 设置失败时执行的回调。
     *
     * @param listener 回调逻辑
     * @return this
     */
    MethodListenable<FR, V> onFailure(GenericFailureFutureResultListener<FR, V> listener);

    /**
     * 设置无论成功或失败都执行的回调
     *
     * @param listener 回调逻辑
     * @return this
     */
    MethodListenable<FR, V> onComplete(GenericFutureResultListener<FR, V> listener);
}
