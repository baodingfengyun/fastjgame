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

import com.wjybxx.fastjgame.net.common.RpcCall;
import com.wjybxx.fastjgame.net.common.RpcClient;
import com.wjybxx.fastjgame.net.common.RpcFutureResult;
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
    private RpcCall<V> call;

    /**
     * @param call 一般来讲，是用于转发的RpcCall
     */
    public DefaultRpcMethodHandle(RpcCall<V> call) {
        this.call = call;
    }

    /**
     * 该方法是生成的代码调用的。
     */
    public DefaultRpcMethodHandle(int methodKey, List<Object> methodParams, int lazyIndexes, int preIndexes) {
        this.call = new RpcCall<>(methodKey, methodParams, lazyIndexes, preIndexes);
    }

    @Override
    public RpcCall<V> getCall() {
        return call;
    }

    @Override
    public RpcMethodHandle<V> router(RpcRouter<V> router) {
        this.call = router.route(call).getCall();
        return this;
    }

    @Override
    public void send(@Nonnull RpcClient client) throws IllegalStateException {
        client.send(call);
    }

    @Override
    public void sendAndFlush(RpcClient client) {
        client.sendAndFlush(call);
    }

    @Override
    public void broadcast(@Nonnull Iterable<RpcClient> clientGroup) throws IllegalStateException {
        for (RpcClient client : clientGroup) {
            client.send(call);
        }
    }

    @Override
    public final TimeoutMethodListenable<RpcFutureResult<V>, V> call(@Nonnull RpcClient client) {
        final TimeoutMethodListenable<RpcFutureResult<V>, V> listenable = new DefaultTimeoutMethodListenable<>();
        client.<V>call(this.call).addListener(listenable);
        return listenable;
    }

    @Override
    public TimeoutMethodListenable<RpcFutureResult<V>, V> callAndFlush(@Nonnull RpcClient client) {
        final TimeoutMethodListenable<RpcFutureResult<V>, V> listenable = new DefaultTimeoutMethodListenable<>();
        client.<V>callAndFlush(call).addListener(listenable);
        return listenable;
    }

    @Override
    public V syncCall(@Nonnull RpcClient client) throws CompletionException {
        return client.syncCall(call);
    }

}
