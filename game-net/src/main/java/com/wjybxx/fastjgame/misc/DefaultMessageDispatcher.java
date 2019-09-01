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

import com.google.protobuf.AbstractMessage;
import com.wjybxx.fastjgame.net.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * {@link MessageFunctionRegistry}的默认实现。
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/24
 * github - https://github.com/hl845740757
 */
public class DefaultMessageDispatcher implements MessageFunctionRegistry, MessageDispatcher {

	private static final Logger logger = LoggerFactory.getLogger(DefaultMessageDispatcher.class);

	/**
	 * 类型到处理器的映射。
	 */
	private final Map<Class<?>, MessageFunction<?>> handlerMap = new IdentityHashMap<>(512);

	@SuppressWarnings("unchecked")
	@Override
	public final <T extends AbstractMessage> void register(@Nonnull Class<T> clazz, @Nonnull MessageFunction<T> handler) {
		final MessageFunction<?> existHandler = handlerMap.get(clazz);
		// 该类型目前还没有被注册
		if (existHandler == null) {
			handlerMap.put(clazz, handler);
			return;
		}
		if (existHandler instanceof CompositeMessageFunction) {
			((CompositeMessageFunction)existHandler).addHandler(handler);
		} else {
			// 已存在该类型的处理器了，我们提供CompositeMessageHandler将其封装为统一结构
			handlerMap.put(clazz, new CompositeMessageFunction(existHandler, handler));
		}
	}

	@Override
	public void release() {
		handlerMap.clear();
	}

	@Override
	public final <T extends AbstractMessage> void post(@Nonnull Session session, @Nonnull T message) {
		@SuppressWarnings("unchecked")
		final MessageFunction<T> messageFunction = (MessageFunction<T>) handlerMap.get(message.getClass());
		if (null == messageFunction) {
			logger.warn("{} - {} send unregistered message {}",
					session.remoteRole(), session.remoteGuid(), message.getClass().getName());
			return;
		}
		try {
			messageFunction.onMessage(session, message);
		} catch (Exception e){
			logger.warn("Handler onMessage caught exception!, handler {}, message {}",
					messageFunction.getClass().getName(), message.getClass().getName(), e);
		}
	}
}
