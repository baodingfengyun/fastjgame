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
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/6
 * github - https://github.com/hl845740757
 */
public class DefaultTimeoutMethodListenable<FR extends TimeoutFutureResult<V>, V> extends DefaultMethodListenable<FR, V> implements TimeoutMethodListenable<FR, V> {

    @Override
    public TimeoutMethodListenable<FR, V> onTimeout(GenericTimeoutFutureResultListener<FR, V> listener) {
        addListener(listener);
        return this;
    }

    @Override
    public TimeoutMethodListenable<FR, V> onSuccess(GenericSuccessFutureResultListener<FR, V> listener) {
        super.onSuccess(listener);
        return this;
    }

    @Override
    public TimeoutMethodListenable<FR, V> onFailure(GenericFailureFutureResultListener<FR, V> listener) {
        super.onFailure(listener);
        return this;
    }

    @Override
    public TimeoutMethodListenable<FR, V> onComplete(GenericFutureResultListener<FR, V> listener) {
        super.onComplete(listener);
        return this;
    }
}
