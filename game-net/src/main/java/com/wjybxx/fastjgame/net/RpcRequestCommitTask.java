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
 * rpc请求任务
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/8
 * github - https://github.com/hl845740757
 */
public class RpcRequestCommitTask implements CommitTask {

	private Session session;
	/** 消息分发器 */
	private ProtocolDispatcher protocolDispatcher;
	/** rpc请求编号，用于返回消息 */
	public long requestGuid;
	/** 是否rpc同步调用，是否加急 */
	public boolean sync;
	/** Rpc请求内容 */
	private Object request;

	public RpcRequestCommitTask(Session session, ProtocolDispatcher protocolDispatcher, long requestGuid, boolean sync, Object request) {
		this.session = session;
		this.protocolDispatcher = protocolDispatcher;
		this.requestGuid = requestGuid;
		this.sync = sync;
		this.request = request;
	}

	@Override
	public void run() {
		final RpcResponseChannel<?> responseChannel = session.sender().newResponseChannel(requestGuid, sync);
		protocolDispatcher.postRpcRequest(session, request, responseChannel);
	}
}
