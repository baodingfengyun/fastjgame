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

package com.wjybxx.fastjgame.net;

import javax.annotation.Nonnull;

/**
 * 带有缓冲区的sender的模板实现。
 * @apiNote
 * 注意：如果任务无法发送，必须调用{@link SenderTask#cancel()}执行取消逻辑。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/9/4
 * github - https://github.com/hl845740757
 */
public abstract class AbstractBufferedSender extends AbstractSender{

	protected AbstractBufferedSender(AbstractSession session) {
		super(session);
	}

	@Override
	protected void doSendMessage(@Nonnull OneWayMessageTask oneWayMessageTask) {
		offerSenderTask(oneWayMessageTask);
	}

	@Override
	protected void doSendAsyncRpcRequest(RpcRequestTask rpcRequestTask) {
		offerSenderTask(rpcRequestTask);
	}

	@Override
	protected void doSendAsyncRpcResponse(RpcResponseTask rpcResponseTask) {
		offerSenderTask(rpcResponseTask);
	}

	/**
	 * 子类通过实现该方法实现自己的缓冲策略！
	 *
	 * @param task 一个数据发送请求
	 */
	protected abstract void offerSenderTask(SenderTask task);

}
