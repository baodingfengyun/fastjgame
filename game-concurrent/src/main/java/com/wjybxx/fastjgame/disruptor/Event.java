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

package com.wjybxx.fastjgame.disruptor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 事件对象
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/24
 * github - https://github.com/hl845740757
 */
public final class Event implements AutoCloseable{

	/**
	 * 事件类型
	 */
	private EventType type;
	/**
	 * 事件参数
	 */
	private EventParam param;

	@Nonnull
	public EventType getType() {
		return type;
	}

	public void setType(EventType type) {
		this.type = type;
	}

	@Nullable
	public EventParam getParam() {
		return param;
	}

	public void setParam(EventParam param) {
		this.param = param;
	}

	/**
	 * help GC
	 * -- Disruptor的RingBuffer会始终持有Event对象，如果Event对象不进行清理，会导致一定程序的内存泄漏。
	 */
	@Override
	public void close() {
		this.type = null;
		this.param = null;
	}
}
