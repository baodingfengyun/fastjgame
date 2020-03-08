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

package com.wjybxx.fastjgame.net.rpc;

import com.wjybxx.fastjgame.utils.concurrent.FailedFutureListener;
import com.wjybxx.fastjgame.utils.concurrent.FutureListener;
import com.wjybxx.fastjgame.utils.concurrent.SucceededFutureListener;
import com.wjybxx.fastjgame.utils.concurrent.timeout.TimeoutFuture;

import javax.annotation.Nonnull;
import java.util.concurrent.Executor;

/**
 * Rpc调用的future。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/3
 * github - https://github.com/hl845740757
 */
public interface RpcFuture<V> extends TimeoutFuture<V> {

    /**
     * 当cause为{@link com.wjybxx.fastjgame.net.exception.RpcTimeoutException}时表示超时
     */
    @Override
    boolean isTimeout();

    /**
     * 查询是否是rpc执行异常
     *
     * @return 如果为异常信息为：{@link com.wjybxx.fastjgame.net.exception.RpcException}，则返回true
     */
    boolean isRpcException();

    /**
     * {@link #isRpcException()}为true的时候，则返回对应的错误码。
     * 否则返回null。
     *
     * @return errorCode
     */
    RpcErrorCode errorCode();

    // 仅用于语法支持
    @Override
    RpcFuture<V> onComplete(@Nonnull FutureListener<? super V> listener);

    @Override
    RpcFuture<V> onComplete(@Nonnull FutureListener<? super V> listener, @Nonnull Executor bindExecutor);

    @Override
    RpcFuture<V> onSuccess(@Nonnull SucceededFutureListener<? super V> listener);

    @Override
    RpcFuture<V> onSuccess(@Nonnull SucceededFutureListener<? super V> listener, @Nonnull Executor bindExecutor);

    @Override
    RpcFuture<V> onFailure(@Nonnull FailedFutureListener<? super V> listener);

    @Override
    RpcFuture<V> onFailure(@Nonnull FailedFutureListener<? super V> listener, @Nonnull Executor bindExecutor);
}
