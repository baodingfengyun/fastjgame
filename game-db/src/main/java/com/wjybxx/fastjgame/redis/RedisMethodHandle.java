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

package com.wjybxx.fastjgame.redis;

import com.wjybxx.fastjgame.async.FlushableMethodHandle;
import com.wjybxx.fastjgame.concurrent.FutureResult;
import com.wjybxx.fastjgame.concurrent.GenericFailureFutureResultListener;
import com.wjybxx.fastjgame.concurrent.GenericFutureResultListener;
import com.wjybxx.fastjgame.concurrent.GenericSuccessFutureResultListener;

import javax.annotation.Nonnull;
import java.util.concurrent.ExecutionException;

/**
 * redis方法句柄，执行主体为{@link RedisServiceHandle}
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/1/9
 * github - https://github.com/hl845740757
 */
public interface RedisMethodHandle<V> extends FlushableMethodHandle<RedisServiceHandle, FutureResult<V>, V> {

    @Override
    void execute(@Nonnull RedisServiceHandle serviceHandle);

    @Override
    void executeAndFlush(@Nonnull RedisServiceHandle serviceHandle);

    @Override
    void call(@Nonnull RedisServiceHandle serviceHandle);

    @Override
    void callAndFlush(@Nonnull RedisServiceHandle serviceHandle);

    @Override
    V syncCall(@Nonnull RedisServiceHandle serviceHandle) throws ExecutionException;

    @Override
    RedisMethodHandle<V> onSuccess(GenericSuccessFutureResultListener<FutureResult<V>, V> listener);

    @Override
    RedisMethodHandle<V> onFailure(GenericFailureFutureResultListener<FutureResult<V>, V> listener);

    @Override
    RedisMethodHandle<V> onComplete(GenericFutureResultListener<FutureResult<V>, V> listener);
}
