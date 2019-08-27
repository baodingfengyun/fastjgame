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
import java.util.LinkedList;
import java.util.List;

/**
 * 为多个MessageHandler提供一个抽象视图。
 * 当一个消息被多个地方监听处理时，使用该对象进行简化。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/24
 * github - https://github.com/hl845740757
 */
public final class CompositeMessageFunction<T extends AbstractMessage> implements MessageFunction<T> {

	private static final Logger logger = LoggerFactory.getLogger(CompositeMessageFunction.class);

	/**
	 * 该handler管理的所有子节点
	 */
	private final List<MessageFunction<T>> children = new LinkedList<>();

	public CompositeMessageFunction() {

	}

	public CompositeMessageFunction(@Nonnull MessageFunction<T> first, @Nonnull MessageFunction<T> second) {
		children.add(first);
		children.add(second);
	}

	public CompositeMessageFunction<T> addHandler(@Nonnull MessageFunction<T> handler) {
		children.add(handler);
		return this;
	}

	@Override
	public void onMessage(Session session, T message) throws Exception {
		for (MessageFunction<T> handler:children) {
			try {
				handler.onMessage(session, message);
			} catch (Exception e) {
				logger.warn("Child onMessage caught exception, child {}, message {}",
						handler.getClass().getName(), message.getClass().getName(), e);
			}
		}
	}
}
