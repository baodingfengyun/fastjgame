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
import com.wjybxx.fastjgame.utils.ConcurrentUtils;

import java.util.LinkedList;
import java.util.List;

/**
 * 为多个RpcCallback提供一个单一的视图。
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/19
 * github - https://github.com/hl845740757
 */
public class CompositeRpcCallback<V> implements RpcCallback {

	private final List<RpcCallback> rpcCallbackList = new LinkedList<>();

	public CompositeRpcCallback() {
	}

	public CompositeRpcCallback(RpcCallback first, RpcCallback second) {
		rpcCallbackList.add(first);
		rpcCallbackList.add(second);
	}

	@Override
	public void onComplete(RpcResponse rpcResponse) {
		for (RpcCallback rpcCallback : rpcCallbackList) {
			ConcurrentUtils.safeExecute((Runnable)() -> rpcCallback.onComplete(rpcResponse));
		}
	}

	/**
	 * 只有成功才执行该方法
	 * @return this
	 */
	public CompositeRpcCallback<V> ifSuccess(SucceedRpcCallback<V> rpcCallback) {
		rpcCallbackList.add(rpcCallback);
		return this;
	}

	/**
	 * 只有失败才执行该方法
	 * @return this
	 */
	public CompositeRpcCallback<V> ifFailure(FailedRpcCallback rpcCallback) {
		rpcCallbackList.add(rpcCallback);
		return this;
	}

	/**
	 * 无论成功失败都会执行该方法
	 * @return this
	 */
	public CompositeRpcCallback<V> any(RpcCallback rpcCallback) {
		rpcCallbackList.add(rpcCallback);
		return this;
	}

	/**
	 * 删除第一个匹配的回调
	 * @param callback 回调
	 */
	public boolean remove(RpcCallback callback) {
		return rpcCallbackList.remove(callback);
	}
}
