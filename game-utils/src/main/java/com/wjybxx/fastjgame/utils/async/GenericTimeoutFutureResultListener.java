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

package com.wjybxx.fastjgame.utils.async;

import com.wjybxx.fastjgame.utils.concurrent.timeout.TimeoutFutureResult;

/**
 * 超时结果监听器
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/1/9
 * github - https://github.com/hl845740757
 */
@FunctionalInterface
public interface GenericTimeoutFutureResultListener<FR extends TimeoutFutureResult<V>, V> extends GenericFutureResultListener<FR, V> {

    @Override
    default void onComplete(FR futureResult) {
        if (futureResult.isTimeout()) {
            onTimeout(futureResult);
        }
    }

    /**
     * 执行超时逻辑
     */
    void onTimeout(FR timeoutResult);
}
