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

package com.wjybxx.fastjgame.net.rpc;

import com.wjybxx.fastjgame.net.exception.RpcException;
import com.wjybxx.fastjgame.net.exception.RpcTimeoutException;
import com.wjybxx.fastjgame.utils.concurrent.DefaultLocalPromise;
import com.wjybxx.fastjgame.utils.concurrent.EventLoop;
import com.wjybxx.fastjgame.utils.concurrent.FutureListener;

import javax.annotation.Nonnull;
import java.util.concurrent.Executor;

/**
 * 线程绑定版的{@link RpcFuture}
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/3/8
 * github - https://github.com/hl845740757
 */
public class DefaultLocalRpcPromise<V> extends DefaultLocalPromise<V> implements RpcPromise<V> {

    public DefaultLocalRpcPromise(EventLoop appEventLoop) {
        super(appEventLoop);
    }

    @Override
    public boolean isTimeout() {
        return cause() instanceof RpcTimeoutException;
    }

    @Override
    public boolean isRpcException() {
        return isRpcException0(cause());
    }

    static boolean isRpcException0(Throwable cause) {
        return cause instanceof RpcException;
    }

    @Override
    public RpcErrorCode getErrorCode() {
        return getErrorCode0(cause());
    }

    static RpcErrorCode getErrorCode0(Throwable cause) {
        if (cause instanceof RpcException) {
            return ((RpcException) cause).getErrorCode();
        }
        return RpcErrorCode.UNKNOWN;
    }

    @Override
    public DefaultLocalRpcPromise<V> addListener(@Nonnull FutureListener<? super V> listener) {
        super.addListener(listener);
        return this;
    }

    @Override
    public DefaultLocalRpcPromise<V> addListener(@Nonnull FutureListener<? super V> listener, @Nonnull Executor bindExecutor) {
        super.addListener(listener, bindExecutor);
        return this;
    }

}
