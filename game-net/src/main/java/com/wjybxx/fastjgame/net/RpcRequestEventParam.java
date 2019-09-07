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

import io.netty.channel.Channel;

/**
 * RPC请求事件参数。 {@link NetPackageType#RPC_REQUEST}
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/30
 * github - https://github.com/hl845740757
 */
public class RpcRequestEventParam extends MessageEventParam{

	/** rpc请求编号，用于返回消息 */
	private long requestGuid;
	/** 是否rpc同步调用，是否加急 */
	private boolean sync;
	/** rpc请求内容 */
	private Object request;

	public RpcRequestEventParam(Channel channel, long localGuid, long remoteGuid, long ack, long sequence, long requestGuid, boolean sync, Object request) {
		super(channel, localGuid, remoteGuid, ack, sequence);
		this.requestGuid = requestGuid;
		this.sync = sync;
		this.request = request;
	}

	public long getRequestGuid() {
		return requestGuid;
	}

	public boolean isSync() {
		return sync;
	}

	public Object getRequest() {
		return request;
	}
}
