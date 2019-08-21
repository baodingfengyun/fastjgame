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

import com.wjybxx.fastjgame.net.RpcCallback;
import com.wjybxx.fastjgame.net.RpcResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 调用成功才执行的Rpc回调。失败时仅仅打印一条日志。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/19
 * github - https://github.com/hl845740757
 */
@FunctionalInterface
public interface SucceedRpcCallback<V> extends RpcCallback {

	Logger logger = LoggerFactory.getLogger(SucceedRpcCallback.class);

	@SuppressWarnings("unchecked")
	@Override
	default void onComplete(RpcResponse rpcResponse) {
		if (rpcResponse.isSuccess()) {
			onSuccess((V) rpcResponse.getBody());
		} else {
			logger.warn("rpc call failure." + rpcResponse.getResultCode());
		}
	}

	/**
	 * 当执行成功时
	 * @param value 调用结果
	 */
	 void onSuccess(V value);
}
