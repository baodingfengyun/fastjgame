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
 * Rpc请求的上下文默认实现
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/13
 * github - https://github.com/hl845740757
 */
public class DefaultRpcRequestContext implements RpcRequestContext{

	/** 是否rpc同步调用，是否加急 */
	public final boolean sync;
	/** rpc请求编号，用于返回消息 */
	public final long requestGuid;

	public DefaultRpcRequestContext(boolean sync, long requestGuid) {
		this.sync = sync;
		this.requestGuid = requestGuid;
	}
}
