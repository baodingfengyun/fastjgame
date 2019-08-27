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

import javax.annotation.Nonnull;

/**
 * 事件处理器注册表
 * @author houlei
 * @version 1.0
 * date - 2019/8/27
 */
public interface EventHandlerRegistry {

	/**
	 * 注册一个事件的观察者，正常情况下，该方法由生成的代码调用。
	 * 当然也可以手动注册一些事件，即不使用注解。
	 *
	 * @param eventType 关注的事件类型
	 * @param handler 事件处理器
	 * @param <T> 事件的类型
	 */
	<T> void register(@Nonnull Class<T> eventType, @Nonnull EventHandler<T> handler);
}
