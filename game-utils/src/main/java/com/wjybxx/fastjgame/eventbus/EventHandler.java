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
 * 事件处理器，主要用于实验Lambda表达式代替反射调用，lambda表达式由注解处理器生成
 *
 * @author houlei
 * @version 1.0
 * date - 2019/8/23
 */
public interface EventHandler<T> {

	/**
	 * 当出现一个订阅的事件
	 * @param event 订阅的事件
	 */
	void onEvent(@Nonnull T event) throws Exception;

}
