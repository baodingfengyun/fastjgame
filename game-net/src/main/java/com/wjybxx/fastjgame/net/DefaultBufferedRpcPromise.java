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

import com.wjybxx.fastjgame.concurrent.BlockingOperationException;
import com.wjybxx.fastjgame.concurrent.EventLoop;
import com.wjybxx.fastjgame.eventloop.NetEventLoop;
import com.wjybxx.fastjgame.utils.EventLoopUtils;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/16
 * github - https://github.com/hl845740757
 */
public class DefaultBufferedRpcPromise extends DefaultRpcPromise implements BufferedRpcPromise {

	/** rpc请求是否已发送出去，是否已提交到网络层，其实可以是非volatile的，因为只有用户线程会访问 */
	private volatile boolean sent = false;
	/** 所属的Sender */
	private BufferedSender bufferedSender;

	public DefaultBufferedRpcPromise(NetEventLoop workerEventLoop, EventLoop userEventLoop, long timeoutMs, BufferedSender bufferedSender) {
		super(workerEventLoop, userEventLoop, timeoutMs);
		this.bufferedSender = bufferedSender;
	}

	@Override
	public void setSent() {
		sent = true;
	}

	@Override
	protected void checkDeadlock() {
		// 注意：如果sent不是volatile的，那么getUserEventLoop().inEventLoop()必须放前面
		if (getUserEventLoop().inEventLoop() && !sent) {
			// 讲道理这里一定能发送出去
			bufferedSender.flush();
			// 如果没发送出去，抛出阻塞异常
			if (!sent) {
				throw new BlockingOperationException();

			}
		} else {
			EventLoopUtils.checkDeadLock(executor());
		}
	}
}
