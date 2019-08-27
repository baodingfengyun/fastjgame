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

import javax.annotation.Nonnull;

/**
 * {@link MessageFunction}注册器。
 * @author houlei
 * @version 1.0
 * date - 2019/8/24
 */
public interface MessageFunctionRegistry {

	/**
	 * 注册一个消息的处理策略
	 * @param clazz 消息的类型
	 * @param handler 消息对应的处理器
	 * @param <T> 消息的类型
	 */
	<T extends AbstractMessage> void register(@Nonnull Class<T> clazz, @Nonnull MessageFunction<T> handler);

}
