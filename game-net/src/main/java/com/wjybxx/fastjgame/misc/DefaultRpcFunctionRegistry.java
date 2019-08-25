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

import com.wjybxx.fastjgame.net.RpcResponseChannel;
import com.wjybxx.fastjgame.net.Session;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.jetbrains.annotations.NotNull;
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
public class DefaultRpcFunctionRegistry implements RpcFunctionRegistry {

	private static final Logger logger = LoggerFactory.getLogger(DefaultRpcFunctionRegistry.class);

	/**
	 * 所有的Rpc请求处理函数, methodKey -> rpcFunction
	 */
	private final Int2ObjectMap<RpcFunction> functionInfoMap = new Int2ObjectOpenHashMap<>(512);

	@Override
	public void register(int methodKey, @NotNull RpcFunction function) {
		// rpc请求id不可以重复，编译时保证了生成的代码不会重复，但是手动注册不在检测范围内
		if (functionInfoMap.containsKey(methodKey)) {
			throw new IllegalArgumentException("methodKey " + methodKey + " is already registered!");
		}
		functionInfoMap.put(methodKey, function);
	}

	/**
	 * 分发一个rpc调用
	 * @param session 所在的会话
	 * @param rpcCall rpc调用信息
	 * @param rpcResponseChannel 如果需要返回结果的话，使用该对象返回值。
	 */
	@SuppressWarnings("unchecked")
	public final void dispatchRpcRequest(@Nonnull Session session, @Nonnull RpcCall rpcCall, @Nonnull RpcResponseChannel<?> rpcResponseChannel) {
		final int methodKey = rpcCall.getMethodKey();
		final List<Object> params = rpcCall.getMethodParams();
		final RpcFunction rpcFunction = functionInfoMap.get(methodKey);
		if (null == rpcFunction) {
			// 不打印参数详情，消耗可能较大，注意：这里的参数大小和真实的方法参数大小不一定一样，主要是ResponseChannel和Session不需要客户端传。
			logger.warn("{} - {} send unregistered request, methodKey={}, parameters size={}",
					session.remoteRole(), session.remoteGuid(), methodKey, params.size());
			return;
		}
		try {
			rpcFunction.call(session, params, rpcResponseChannel);
		} catch (Exception e){
			logger.warn("handle {} - {} rpcCall caught exception, methodKey={}",
					session.remoteRole(), session.remoteGuid(), methodKey, e);
		}
	}
}
