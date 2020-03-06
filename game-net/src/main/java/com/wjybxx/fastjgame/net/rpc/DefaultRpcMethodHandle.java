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

import com.wjybxx.fastjgame.utils.concurrent.NonBlockingFuture;

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
    private RpcMethodSpec<V> rpcMethodSpec;

    public DefaultRpcMethodHandle(RpcMethodSpec<V> rpcMethodSpec) {
        this.rpcMethodSpec = rpcMethodSpec;
    }

    /**
     * 该方法是生成的代码调用的。
     */
    public DefaultRpcMethodHandle(short serviceId, short methodId, List<Object> methodParams, int lazyIndexes, int preIndexes) {
        this.rpcMethodSpec = new RpcMethodSpec<>(serviceId, methodId, methodParams, lazyIndexes, preIndexes);
    }

    @Override
    public RpcMethodSpec<V> getRpcMethodSpec() {
        return rpcMethodSpec;
    }

    @Override
    public RpcMethodHandle<V> router(RpcRouter<V> router) {
        this.rpcMethodSpec = router.route(rpcMethodSpec).getRpcMethodSpec();
        return this;
    }

    @Override
    public void send(@Nonnull RpcClient client) throws IllegalStateException {
        client.send(rpcMethodSpec);
    }

    @Override
    public void sendAndFlush(RpcClient client) {
        client.sendAndFlush(rpcMethodSpec);
    }

    @Override
    public void broadcast(@Nonnull Iterable<RpcClient> clientGroup) throws IllegalStateException {
        for (RpcClient client : clientGroup) {
            client.send(rpcMethodSpec);
        }
    }

    @Override
    public final NonBlockingFuture<V> call(@Nonnull RpcClient client) {
        return client.call(this.rpcMethodSpec);
    }

    @Override
    public NonBlockingFuture<V> callAndFlush(@Nonnull RpcClient client) {
        return client.callAndFlush(rpcMethodSpec);
    }

    @Override
    public V syncCall(@Nonnull RpcClient client) throws CompletionException {
        return client.syncCall(rpcMethodSpec);
    }

}
