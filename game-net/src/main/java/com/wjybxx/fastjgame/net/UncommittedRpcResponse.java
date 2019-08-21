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

/**
 * 已接收但还未提交给应用层的Rpc响应
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/13
 * github - https://github.com/hl845740757
 */
public class UncommittedRpcResponse implements UncommittedMessage{

	private final RpcResponse rpcResponse;
	private final RpcCallback rpcCallback;

	public UncommittedRpcResponse(RpcResponse rpcResponse, RpcCallback rpcCallback) {
		this.rpcResponse = rpcResponse;
		this.rpcCallback = rpcCallback;
	}

	@Override
	public void commit(Session session, MessageHandler messageHandler) throws Exception {
		rpcCallback.onComplete(rpcResponse);
	}

	@Override
	public void onRejected(Session session) {
		// do nothing
	}
}
