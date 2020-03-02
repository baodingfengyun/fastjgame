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

import com.wjybxx.fastjgame.net.common.RpcErrorCode;
import com.wjybxx.fastjgame.net.common.RpcRequest;
import com.wjybxx.fastjgame.net.common.RpcResponseChannel;
import com.wjybxx.fastjgame.net.session.Session;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.List;

/**
 * 默认的Rpc函数注册表。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/21
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public class DefaultRpcRequestDispatcher implements RpcFunctionRegistry, RpcRequestDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(DefaultRpcRequestDispatcher.class);

    /**
     * 所有的Rpc请求处理函数, methodKey -> rpcFunction
     */
    private final Int2ObjectMap<RpcFunction> functionInfoMap = new Int2ObjectOpenHashMap<>(512);

    @Override
    public final void register(short serviceId, short methodId, @Nonnull RpcFunction function) {
        // rpc请求id不可以重复
        final int methodKey = calMethodKey(serviceId, methodId);
        if (functionInfoMap.containsKey(methodKey)) {
            throw new IllegalArgumentException("methodKey " + methodKey + " is already registered!");
        }
        functionInfoMap.put(methodKey, function);
    }

    @SuppressWarnings("unchecked")
    @Override
    public final void post(@Nonnull Session session, @Nonnull RpcRequest rpcRequest, @Nonnull RpcResponseChannel<?> rpcResponseChannel) {
        final int methodKey = calMethodKey(rpcRequest.getServiceId(), rpcRequest.getMethodId());
        final List<Object> params = rpcRequest.getMethodParams();
        final RpcFunction rpcFunction = functionInfoMap.get(methodKey);
        if (null == rpcFunction) {
            rpcResponseChannel.writeFailure(RpcErrorCode.SERVER_EXCEPTION, "methodKey " + methodKey);
            logger.warn("{} send unregistered request, methodKey={}, parameters={}",
                    session.sessionId(), methodKey, params);
            return;
        }
        try {
            rpcFunction.call(session, params, rpcResponseChannel);
        } catch (Exception e) {
            logger.warn("handle {} rpcRequest caught exception, methodKey={}, parameters={}",
                    session.sessionId(), methodKey, params);
        }
    }

    /**
     * 释放所有捕获的对象，避免内存泄漏
     */
    public final void release() {
        functionInfoMap.clear();
    }

    private static int calMethodKey(short serviceId, short methodId) {
        return serviceId * 10000 + methodId;
    }
}
