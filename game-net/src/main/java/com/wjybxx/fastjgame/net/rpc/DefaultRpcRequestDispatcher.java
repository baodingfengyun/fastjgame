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

import com.wjybxx.fastjgame.net.session.Session;
import com.wjybxx.fastjgame.utils.concurrent.Promise;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * rpc请求分发的默认实现
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/26
 * github - https://github.com/hl845740757
 */
public class DefaultRpcRequestDispatcher implements RpcMethodProxyRegistry, RpcRequestDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(DefaultRpcRequestDispatcher.class);

    /**
     * 所有的Rpc请求处理函数, methodKey -> methodProxy
     */
    private final Int2ObjectMap<RpcMethodProxy> proxyMapping = new Int2ObjectOpenHashMap<>(512);

    public DefaultRpcRequestDispatcher() {

    }

    @Override
    public final void register(short serviceId, short methodId, @Nonnull RpcMethodProxy proxy) {
        // rpc请求id不可以重复
        final int methodKey = calMethodKey(serviceId, methodId);
        if (proxyMapping.containsKey(methodKey)) {
            throw new IllegalArgumentException("methodKey " + methodKey + " is already registered!");
        }
        proxyMapping.put(methodKey, proxy);
    }

    private static int calMethodKey(short serviceId, short methodId) {
        return serviceId * 10000 + methodId;
    }

    /**
     * 释放所有捕获的对象，避免内存泄漏
     */
    public final void release() {
        proxyMapping.clear();
    }

    @SuppressWarnings("unchecked")
    @Override
    public final <V> void post(Session session, @Nullable RpcMethodSpec<V> request, @Nonnull Promise<V> promise) {
        if (null == request) {
            logger.warn("{} send null request", session.sessionId());
            return;
        }
        if (request instanceof DefaultRpcMethodSpec) {
            postImp(session, (DefaultRpcMethodSpec) request, promise);
        } else {
            post0(session, request, promise);
        }
    }

    private <T> void postImp(@Nonnull Session session, @Nonnull DefaultRpcMethodSpec<T> rpcMethodSpec, @Nonnull Promise<T> promise) {
        final int methodKey = calMethodKey(rpcMethodSpec.getServiceId(), rpcMethodSpec.getMethodId());
        final List<Object> params = rpcMethodSpec.getMethodParams();
        @SuppressWarnings("unchecked") final RpcMethodProxy<T> methodProxy = proxyMapping.get(methodKey);
        if (null == methodProxy) {
            promise.tryFailure("Unknown methodKey " + methodKey);
            logger.warn("{} send unregistered request, methodKey={}, parameters={}",
                    session.sessionId(), methodKey, params);
            return;
        }
        try {
            methodProxy.invoke(session, params, promise);
        } catch (Exception e) {
            logger.warn("handle {} rpcRequest caught exception, methodKey={}, parameters={}",
                    session.sessionId(), methodKey, params);
        }
    }

    /**
     * 如果rpc描述信息不是{@link DefaultRpcMethodSpec}对象，那么需要自己实现分发操作
     */
    protected <V> void post0(Session session, RpcMethodSpec<V> request, Promise<V> promise) {

    }

}
