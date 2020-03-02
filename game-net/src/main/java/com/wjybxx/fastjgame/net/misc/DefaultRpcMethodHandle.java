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

package com.wjybxx.fastjgame.net.misc;

import com.wjybxx.fastjgame.net.common.RpcClient;
import com.wjybxx.fastjgame.net.common.RpcFutureResult;
import com.wjybxx.fastjgame.net.common.RpcRequest;
import com.wjybxx.fastjgame.utils.async.DefaultTimeoutMethodListenable;
import com.wjybxx.fastjgame.utils.async.TimeoutMethodListenable;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.List;
import java.util.concurrent.CompletionException;

/**
 * {@link RpcMethodHandle}的默认实现
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/23
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public class DefaultRpcMethodHandle<V> implements RpcMethodHandle<V> {

    /**
     * 远程方法信息
     */
    private RpcRequest<V> request;

    /**
     * @param request 一般来讲，是用于转发的RpcCall
     */
    public DefaultRpcMethodHandle(RpcRequest<V> request) {
        this.request = request;
    }

    /**
     * 该方法是生成的代码调用的。
     */
    public DefaultRpcMethodHandle(short serviceId, short methodId, List<Object> methodParams, int lazyIndexes, int preIndexes) {
        this.request = new RpcRequest<>(serviceId, methodId, methodParams, lazyIndexes, preIndexes);
    }

    @Override
    public RpcRequest<V> getRequest() {
        return request;
    }

    @Override
    public RpcMethodHandle<V> router(RpcRouter<V> router) {
        this.request = router.route(request).getRequest();
        return this;
    }

    @Override
    public void send(@Nonnull RpcClient client) throws IllegalStateException {
        client.send(request);
    }

    @Override
    public void sendAndFlush(RpcClient client) {
        client.sendAndFlush(request);
    }

    @Override
    public void broadcast(@Nonnull Iterable<RpcClient> clientGroup) throws IllegalStateException {
        for (RpcClient client : clientGroup) {
            client.send(request);
        }
    }

    @Override
    public final TimeoutMethodListenable<RpcFutureResult<V>, V> call(@Nonnull RpcClient client) {
        final TimeoutMethodListenable<RpcFutureResult<V>, V> listenable = new DefaultTimeoutMethodListenable<>();
        client.<V>call(this.request).addListener(listenable);
        return listenable;
    }

    @Override
    public TimeoutMethodListenable<RpcFutureResult<V>, V> callAndFlush(@Nonnull RpcClient client) {
        final TimeoutMethodListenable<RpcFutureResult<V>, V> listenable = new DefaultTimeoutMethodListenable<>();
        client.<V>callAndFlush(request).addListener(listenable);
        return listenable;
    }

    @Override
    public V syncCall(@Nonnull RpcClient client) throws CompletionException {
        return client.syncCall(request);
    }

}
