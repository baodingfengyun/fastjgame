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

package com.wjybxx.fastjgame.eventbus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * EventBus的一个简单实现。
 * 1. 它并不是一个线程安全的对象
 * 2. 它也不是一个标准的EventBus实现，比如就没有取消注册的接口，也没有dispatcher、Registry
 *
 * @author houlei
 * @version 1.0
 * date - 2019/8/23
 */
@NotThreadSafe
public class EventBus {

	private static final Logger logger = LoggerFactory.getLogger(EventBus.class);

	private final Map<Class<?>, List<EventHandler<?>>> handlerMap = new IdentityHashMap<>(512);

	/**
	 * 发布一个事件
	 * @param event 要发布的事件
	 */
	@SuppressWarnings("unchecked")
	public void post(@Nonnull Object event) {
		final List<EventHandler<?>> handlerList = handlerMap.get(event.getClass());
		if (null == handlerList) {
			// 对应的事件处理器可能忘记了注册
			logger.warn("{}'s listeners may forgot register!", event.getClass().getName());
			return;
		}
		for (EventHandler eventHandler:handlerList) {
			try {
				eventHandler.onEvent(event);
			} catch (Exception e){
				// 不能因为某个异常导致其它监听器接收不到事件
				logger.warn("onEvent caught exception! EventInfo {}, handler info {}",
						event.getClass().getName(), eventHandler.getClass().getName(), e);
			}
		}
	}

	/**
	 * 注册一个事件的观察者，正常情况下，该方法由生成的代码调用。
	 * 当然也可以手动注册一些事件，即不使用注解。
	 *
	 * @param eventType 关注的事件类型
	 * @param handler 事件处理器
	 * @param <T> 事件的类型
	 */
	public <T> void register(@Nonnull Class<T> eventType, EventHandler<T> handler) {
		// 如果选用arrayList会有大量空间浪费，最好一点点开始扩容，而且注册应该发送启动阶段。大多数事件类型只有很少的观察者
		final List<EventHandler<?>> handlerList = handlerMap.computeIfAbsent(eventType, clazz -> new ArrayList<>(4));
		handlerList.add(handler);
	}
}
