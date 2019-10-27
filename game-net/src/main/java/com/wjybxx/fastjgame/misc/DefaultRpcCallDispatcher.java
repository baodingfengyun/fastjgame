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

package com.wjybxx.fastjgame.misc;

import com.wjybxx.fastjgame.net.common.RpcResponse;
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
public class DefaultRpcCallDispatcher implements RpcFunctionRegistry, RpcCallDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(DefaultRpcCallDispatcher.class);

    /**
     * 所有的Rpc请求处理函数, methodKey -> rpcFunction
     */
    private final Int2ObjectMap<RpcFunction> functionInfoMap = new Int2ObjectOpenHashMap<>(512);

    @Override
    public final void register(int methodKey, @Nonnull RpcFunction function) {
        // rpc请求id不可以重复
        if (functionInfoMap.containsKey(methodKey)) {
            throw new IllegalArgumentException("methodKey " + methodKey + " is already registered!");
        }
        functionInfoMap.put(methodKey, function);
    }

    @SuppressWarnings("unchecked")
    @Override
    public final void post(@Nonnull Session session, @Nonnull RpcCall rpcCall, @Nonnull RpcResponseChannel<?> rpcResponseChannel) {
        final int methodKey = rpcCall.getMethodKey();
        final List<Object> params = rpcCall.getMethodParams();
        final RpcFunction rpcFunction = functionInfoMap.get(methodKey);
        if (null == rpcFunction) {
            rpcResponseChannel.write(RpcResponse.BAD_REQUEST);
            // 不打印参数详情，消耗可能较大，注意：这里的参数大小和真实的方法参数大小不一定一样，主要是ResponseChannel和Session不需要客户端传。
            logger.warn("{} send unregistered request, methodKey={}, parameters size={}",
                    session.sessionId(), methodKey, params.size());
            return;
        }
        try {
            rpcFunction.call(session, params, rpcResponseChannel);
        } catch (Exception e) {
            logger.warn("handle {} rpcCall caught exception, methodKey={}",
                    session.sessionId(), methodKey, e);
        }
    }

    /**
     * 释放所有捕获的对象，避免内存泄漏
     */
    public final void release() {
        functionInfoMap.clear();
    }
}
