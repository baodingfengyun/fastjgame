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

import com.wjybxx.fastjgame.utils.annotation.UnstableApi;
import com.wjybxx.fastjgame.utils.concurrent.FutureListener;
import com.wjybxx.fastjgame.utils.concurrent.timeout.TimeoutFuture;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.Executor;

/**
 * Rpc调用的future。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/3
 * github - https://github.com/hl845740757
 * @apiNote Rpc请求具有时效性，因此{@link #get()},{@link #await()}系列方法，不会无限阻塞，都会在超时时间到达后醒来。
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

    @UnstableApi
    @Nullable
    @Override
    RpcFutureResult<V> getAsResult();

    @Override
    RpcFuture<V> await() throws InterruptedException;

    @Override
    RpcFuture<V> awaitUninterruptibly();

    @Override
    RpcFuture<V> addListener(@Nonnull FutureListener<? super V> listener);

    @Override
    RpcFuture<V> addListener(@Nonnull FutureListener<? super V> listener, @Nonnull Executor bindExecutor);

    @Override
    RpcFuture<V> removeListener(@Nonnull FutureListener<? super V> listener);
}
