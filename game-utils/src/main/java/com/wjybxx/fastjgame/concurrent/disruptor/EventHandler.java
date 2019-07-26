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

/**
 * 事件处理器，注意该处理器不可以执行长时间的阻塞逻辑。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/24
 * github - https://github.com/hl845740757
 */
public interface EventHandler {

	/**
	 * 通知EventHandler启动
	 * @param eventLoop eventHandler所在的事件循环，可以保存下来
	 */
	void startUp(DisruptorEventLoop eventLoop);

	/**
	 * 接收到一个事件
	 */
	void onEvent(Event event) throws Exception;

	/**
	 * 等待事件期间 -- 在一定时间内没有事件时会被调用
	 */
	void onWaitEvent();

	/**
	 * 命令EventHandler关闭
	 */
	void shutdown();
}