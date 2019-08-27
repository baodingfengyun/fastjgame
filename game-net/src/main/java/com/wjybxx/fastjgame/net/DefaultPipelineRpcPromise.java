/*
 *    Copyright 2019 wjybxx
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.wjybxx.fastjgame.net;

import com.wjybxx.fastjgame.concurrent.EventLoop;
import com.wjybxx.fastjgame.eventloop.NetEventLoop;
import com.wjybxx.fastjgame.utils.EventLoopUtils;

/**
 * 默认的PipelineRpcRromise实现
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/16
 * github - https://github.com/hl845740757
 */
public class DefaultPipelineRpcPromise extends DefaultRpcPromise implements PipelineRpcPromise{

	private volatile boolean sent = false;

	public DefaultPipelineRpcPromise(NetEventLoop workerEventLoop, EventLoop userEventLoop, long timeoutMs) {
		super(workerEventLoop, userEventLoop, timeoutMs);
	}

	@Override
	public void setSent() {
		sent = true;
	}

	@Override
	protected void checkDeadlock() {
		if (sent) {
			EventLoopUtils.checkDeadLock(executor());
		} else {
			// 还未发送出去，禁止在上面等待
			EventLoopUtils.checkDeadLock(getUserEventLoop());
		}
	}
}
