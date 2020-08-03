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

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/26
 * github - https://github.com/hl845740757
 */
public class DefaultRpcProcessor implements RpcMethodProxyRegistry, RpcProcessor {

    /**
     * 所有的Rpc请求处理函数, methodKey -> methodProxy
     */
    private final Int2ObjectMap<RpcMethodProxy> proxyMap = new Int2ObjectOpenHashMap<>(512);

    public DefaultRpcProcessor() {

    }

    @Override
    public final void register(short serviceId, short methodId, @Nonnull RpcMethodProxy proxy) {
        // rpc请求id不可以重复
        final int methodKey = calMethodKey(serviceId, methodId);
        if (proxyMap.containsKey(methodKey)) {
            throw new IllegalArgumentException("methodKey " + methodKey + " is already registered!");
        }
        proxyMap.put(methodKey, proxy);
    }

    private static int calMethodKey(short serviceId, short methodId) {
        return serviceId * 10000 + methodId;
    }

    /**
     * 释放所有捕获的对象，避免内存泄漏
     */
    public final void release() {
        proxyMap.clear();
    }

    @Override
    public final Object process(RpcProcessContext context, @Nullable RpcMethodSpec<?> request) throws Exception {
        if (null == request) {
            throw new IllegalArgumentException(context.session().sessionId() + " send null request");
        }

        if (request instanceof DefaultRpcMethodSpec) {
            return postImp(context, (DefaultRpcMethodSpec) request);
        } else {
            return post0(context, request);
        }
    }

    private Object postImp(@Nonnull RpcProcessContext context, @Nonnull DefaultRpcMethodSpec<?> rpcMethodSpec) throws Exception {
        final int methodKey = calMethodKey(rpcMethodSpec.getServiceId(), rpcMethodSpec.getMethodId());
        final RpcMethodProxy methodProxy = proxyMap.get(methodKey);
        if (null == methodProxy) {
            final String msg = String.format("rcv unknown request, session %s, serviceId=%d methodId=%d",
                    context.session().sessionId(), rpcMethodSpec.getServiceId(), rpcMethodSpec.getMethodId());
            throw new IllegalArgumentException(msg);
        }

        final List<Object> params = rpcMethodSpec.getMethodParams();
        try {
            return methodProxy.invoke(context, params);
        } catch (Exception e) {
            final String msg = String.format("invoke caught exception, session %s, serviceId=%d methodId=%d",
                    context.session().sessionId(), rpcMethodSpec.getServiceId(), rpcMethodSpec.getMethodId());
            throw new RuntimeException(msg, e);
        }
    }

    /**
     * 如果rpc描述信息不是{@link DefaultRpcMethodSpec}对象，那么需要自己实现分发操作
     */
    protected Object post0(RpcProcessContext context, RpcMethodSpec<?> request) {
        final String msg = String.format("unknown requestType, session %s, requestType=%s",
                context.session().sessionId(), request.getClass().getName());
        throw new UnsupportedOperationException(msg);
    }

}
