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

package com.wjybxx.fastjgame.net.http;

import com.wjybxx.fastjgame.async.TimeoutMethodHandle;
import com.wjybxx.fastjgame.concurrent.GenericFailureFutureResultListener;
import com.wjybxx.fastjgame.concurrent.GenericFutureResultListener;
import com.wjybxx.fastjgame.concurrent.GenericSuccessFutureResultListener;
import com.wjybxx.fastjgame.concurrent.timeout.GenericTimeoutFutureResultListener;

import javax.annotation.Nonnull;
import java.util.concurrent.ExecutionException;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/1/10
 * github - https://github.com/hl845740757
 */
public interface HttpMethodHandle<V> extends TimeoutMethodHandle<HttpClientProxy, HttpFutureResult<V>, V> {

    @Override
    void execute(@Nonnull HttpClientProxy serviceHandle);

    @Override
    void call(@Nonnull HttpClientProxy serviceHandle);

    @Override
    V syncCall(@Nonnull HttpClientProxy serviceHandle) throws ExecutionException;

    @Override
    HttpMethodHandle<V> onSuccess(GenericSuccessFutureResultListener<HttpFutureResult<V>, V> listener);

    @Override
    HttpMethodHandle<V> onFailure(GenericFailureFutureResultListener<HttpFutureResult<V>, V> listener);

    @Override
    HttpMethodHandle<V> onComplete(GenericFutureResultListener<HttpFutureResult<V>, V> listener);

    @Override
    HttpMethodHandle<V> onTimeout(GenericTimeoutFutureResultListener<HttpFutureResult<V>, V> listener);
}
