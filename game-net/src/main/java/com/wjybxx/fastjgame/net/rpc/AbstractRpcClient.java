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
import com.wjybxx.fastjgame.utils.concurrent.EventLoop;
import com.wjybxx.fastjgame.utils.concurrent.FluentFuture;

/**
 * {@link RpcClient}抽象实现，提供{@link RpcClientInvoker}的默认实现和，提供{@link #newSessionNotFoundFuture(RpcServerSpec)}
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/3/9
 */
public abstract class AbstractRpcClient implements RpcClient {

    /**
     * 默认的监听器执行环境，建议为应用线程
     */
    protected final EventLoop defaultExecutor;
    protected final RpcClientInvoker invoker;

    public AbstractRpcClient(EventLoop defaultExecutor) {
        this.defaultExecutor = defaultExecutor;
        this.invoker = new DefaultRpcClientInvoker();
    }

    /**
     * 当找不到对应的服务信息时，使用该方法可以获取一个失败的future。
     *
     * @param serverSpec 错误的或不支持的服务描述信息
     */
    protected final <V> FluentFuture<V> newSessionNotFoundFuture(RpcServerSpec serverSpec) {
        return defaultExecutor.newFailedFuture(new RpcSessionNotFoundException(serverSpec));
    }
}
