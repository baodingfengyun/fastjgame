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

import java.util.concurrent.ThreadFactory;


/**
 * 全局的EventLoop。它是一个单线程的EventLoop，它不适合处理一些耗时的、阻塞的操作，
 * 仅仅适合处理一些简单的事件，当没有其它的更好的选择时可以使用{@link GlobalEventLoop}。
 *
 * 它会在没有任务后自动的关闭。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/14
 * github - https://github.com/hl845740757
 */
public class GlobalEventLoop extends SingleThreadEventLoop{

	public static final GlobalEventLoop INSTANCE = new GlobalEventLoop(null, (ThreadFactory) Thread::new);

	public GlobalEventLoop(EventLoopGroup parent, ThreadFactory threadFactory) {
		super(parent, threadFactory);
	}

	@Override
	protected void loop() {

	}
}
