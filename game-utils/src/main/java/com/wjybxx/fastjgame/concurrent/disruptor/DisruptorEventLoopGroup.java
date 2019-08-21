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

package com.wjybxx.fastjgame.concurrent.disruptor;

import com.wjybxx.fastjgame.concurrent.EventLoopChooserFactory;
import com.wjybxx.fastjgame.concurrent.MultiThreadEventLoopGroup;
import com.wjybxx.fastjgame.concurrent.RejectedExecutionHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadFactory;

/**
 * Disruptor事件循环组。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/24
 * github - https://github.com/hl845740757
 */
public class DisruptorEventLoopGroup extends MultiThreadEventLoopGroup {

	public DisruptorEventLoopGroup(@Nonnull ThreadFactory threadFactory,
								   @Nonnull RejectedExecutionHandler rejectedExecutionHandler,
								   @Nonnull List<BuildContext> contextList) {
		super(contextList.size(), threadFactory, rejectedExecutionHandler, new LinkedList<>(contextList));
	}

	public DisruptorEventLoopGroup(@Nonnull ThreadFactory threadFactory,
								   @Nonnull RejectedExecutionHandler rejectedExecutionHandler,
								   @Nullable EventLoopChooserFactory chooserFactory,
								   @Nonnull List<BuildContext> contextList) {
		super(contextList.size(), threadFactory, rejectedExecutionHandler, chooserFactory, new LinkedList<>(contextList));
	}

	@Nonnull
	@Override
	public DisruptorEventLoop next() {
		return (DisruptorEventLoop) super.next();
	}

	@Nonnull
	@Override
	protected DisruptorEventLoop newChild(ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler, Object context) {
		@SuppressWarnings("unchecked")
		BuildContext buildContext = ((List<BuildContext>)context).remove(0);
		int ringBufferSize = buildContext.ringBufferSize > 0 ? buildContext.ringBufferSize : DisruptorEventLoop.DEFAULT_RING_BUFFER_SIZE;
		return new DisruptorEventLoop(this, threadFactory, rejectedExecutionHandler, buildContext.eventHandler, ringBufferSize);
	}

	/**
	 * 待优化
	 */
	public static class BuildContext {
		private final int ringBufferSize;
		private final EventHandler eventHandler;

		public BuildContext(EventHandler eventHandler) {
			this(-1, eventHandler);
		}

		public BuildContext(int ringBufferSize, EventHandler eventHandler) {
			this.ringBufferSize = ringBufferSize;
			this.eventHandler = eventHandler;
		}
	}
}
