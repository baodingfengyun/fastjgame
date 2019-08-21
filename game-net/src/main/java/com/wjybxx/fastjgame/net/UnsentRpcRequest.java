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

package com.wjybxx.fastjgame.net;

/**
 * 发送缓冲区中未发送的rpc请求。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/3
 * github - https://github.com/hl845740757
 */
public class UnsentRpcRequest implements UnsentMessage{

	/** Rpc请求内容，至于怎么解释它，不限制 */
	private Object request;
	/** Rpc请求的一些信息 */
	private final RpcPromiseInfo rpcPromiseInfo;

	public UnsentRpcRequest(Object request, RpcPromiseInfo rpcPromiseInfo) {
		this.request = request;
		this.rpcPromiseInfo = rpcPromiseInfo;
	}

	@Override
	public SentMessage build(long sequence, MessageQueue messageQueue) {
		// 发送的时候才申请guid
		long requestGuid = messageQueue.nextRpcRequestGuid();
		// 发送前保存信息
		messageQueue.getRpcPromiseInfoMap().put(requestGuid, rpcPromiseInfo);
		return new SentRpcRequest(sequence, requestGuid, rpcPromiseInfo.sync, request);
	}
}
