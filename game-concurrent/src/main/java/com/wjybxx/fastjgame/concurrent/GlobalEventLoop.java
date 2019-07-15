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

package com.wjybxx.fastjgame.concurrent;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

public class GlobalEventLoop extends SingleThreadEventLoop{

	public static final GlobalEventLoop INSTANCE = new GlobalEventLoop(null, (ThreadFactory) Thread::new);

	public GlobalEventLoop(EventLoopGroup parent, ThreadFactory threadFactory) {
		super(parent, threadFactory);
	}

	public GlobalEventLoop(EventLoopGroup parent, Executor executor) {
		super(parent, executor);
	}

}
