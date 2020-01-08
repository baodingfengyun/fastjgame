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

package com.wjybxx.fastjgame.net.common;

import com.wjybxx.fastjgame.concurrent.EventLoop;
import com.wjybxx.fastjgame.concurrent.FailedFuture;
import com.wjybxx.fastjgame.concurrent.FutureListener;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 已完成的Rpc调用，在它上面的任何监听都将立即执行。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/3
 * github - https://github.com/hl845740757
 */
public class FailedRpcFuture<V> extends FailedFuture<V> implements RpcFuture<V> {

    public FailedRpcFuture(@Nonnull EventLoop notifyExecutor, @Nonnull Throwable cause) {
        super(notifyExecutor, cause);
    }

    @Override
    public boolean isTimeout() {
        // 早已失败，不是超时失败
        return false;
    }

    @Nullable
    @Override
    public RpcFutureResult<V> getAsResult() {
        return new DefaultRpcFutureResult<>(null, cause());
    }

    // ------------------------------------------------ 流式语法支持 ------------------------------------

    @Override
    public RpcFuture<V> await() {
        return this;
    }

    @Override
    public RpcFuture<V> awaitUninterruptibly() {
        return this;
    }

    @Override
    public RpcFuture<V> addListener(@Nonnull FutureListener<? super V> listener) {
        super.addListener(listener);
        return this;
    }

    @Override
    public RpcFuture<V> addListener(@Nonnull FutureListener<? super V> listener, @Nonnull EventLoop bindExecutor) {
        super.addListener(listener, bindExecutor);
        return this;
    }

    @Override
    public RpcFuture<V> removeListener(@Nonnull FutureListener<? super V> listener) {
        super.removeListener(listener);
        return this;
    }

}
