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

import com.wjybxx.fastjgame.net.exception.RpcSessionNotFoundException;
import com.wjybxx.fastjgame.util.concurrent.EventLoop;
import com.wjybxx.fastjgame.util.concurrent.FluentFuture;
import com.wjybxx.fastjgame.util.concurrent.FutureUtils;

/**
 * {@link RpcClient}抽象实现，提供{@link RpcClientInvoker}的默认实现和，提供{@link #newSessionNotFoundFuture(RpcServerSpec)}
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/3/9
 */
public abstract class AbstractRpcClient implements RpcClient {

    protected final EventLoop appEventLoop;
    protected final RpcClientInvoker invoker;

    public AbstractRpcClient(EventLoop appEventLoop) {
        this.appEventLoop = appEventLoop;
        this.invoker = new DefaultRpcClientInvoker();
    }

    /**
     * 当找不到对应的服务信息时，使用该方法可以获取一个失败的future。
     *
     * @param serverSpec 错误的或不支持的服务描述信息
     */
    protected final <V> FluentFuture<V> newSessionNotFoundFuture(RpcServerSpec serverSpec) {
        return FutureUtils.newFailedFuture(new RpcSessionNotFoundException(serverSpec));
    }
}
