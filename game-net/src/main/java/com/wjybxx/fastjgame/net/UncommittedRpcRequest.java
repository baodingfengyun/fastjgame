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
 * 接收缓冲区中未提交的异步Rpc请求。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/8
 * github - https://github.com/hl845740757
 */
public class UncommittedRpcRequest extends AbstractUncommittedMessage{

	/** Rpc请求内容 */
	private final Object request;
	/** 该请求对应的上下文 */
	private final DefaultRpcRequestContext context;

	public UncommittedRpcRequest(Object request, DefaultRpcRequestContext context) {
		this.request = request;
		this.context = context;
	}

	@Override
	public void doCommit(Session session, ProtocolDispatcher protocolDispatcher) {
		protocolDispatcher.postRpcRequest(session, request, context);
	}
}
