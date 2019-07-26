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

import com.wjybxx.fastjgame.concurrent.EventLoop;
import com.wjybxx.fastjgame.concurrent.EventLoopChooserFactory;
import com.wjybxx.fastjgame.concurrent.MultiThreadEventLoopGroup;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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

	private DisruptorEventLoopGroup(@Nonnull ThreadFactory threadFactory, List<BuildContext> contextList) {
		super(contextList.size(), threadFactory, contextList);
	}

	private DisruptorEventLoopGroup(int nThreads, @Nonnull ThreadFactory threadFactory, @Nullable EventLoopChooserFactory chooserFactory,
									List<BuildContext> contextList) {
		super(contextList.size(), threadFactory, chooserFactory, contextList);
	}

	@Nonnull
	@Override
	protected EventLoop newChild(ThreadFactory threadFactory, Object context) {
		BuildContext buildContext = ((List<BuildContext>)context).remove(0);
		if (buildContext.ringBufferSize > 0){
			return new DisruptorEventLoop(this, threadFactory, buildContext.eventHandler, buildContext.ringBufferSize);
		} else {
			return new DisruptorEventLoop(this, threadFactory, buildContext.eventHandler);
		}
	}

	// TODO 这里临时为了报错
	public static class BuildContext {
		private int ringBufferSize = 0;
		private EventHandler eventHandler;
	}
}
