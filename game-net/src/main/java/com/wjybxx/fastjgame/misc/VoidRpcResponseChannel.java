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

package com.wjybxx.fastjgame.misc;

import com.wjybxx.fastjgame.net.*;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

/**
 * 没有返回值的Channel，占位符，表示用户不关心返回值或方法本身无返回值
 * 主要是{@link ProtocolDispatcher#postOneWayMessage(Session, Object)}用的，
 * 可以将一个onMessage伪装成 RpcRequest，这样应用层可以使用相同的接口对待单向消息和rpc请求。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/21
 * github - https://github.com/hl845740757
 */
@ThreadSafe
public class VoidRpcResponseChannel implements RpcResponseChannel<Void> {

	public static final RpcResponseChannel INSTANCE = new VoidRpcResponseChannel();

	@Override
	public void write(@Nonnull RpcResponse rpcResponse) {
		// do nothing
	}

	@Override
	public boolean isVoid() {
		return true;
	}
}
