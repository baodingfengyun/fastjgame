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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 返回rpc结果的通道。
 * 注意：该channel是一次性的，只可以使用一次(返回一次结果)，多次调用将抛出异常。
 * 当该参数在Rpc方法的参数中出现时，代码生成工具会捕获泛型T的类型，作为返回类型，且{@link RpcResponseChannel}不会出现在代理方法参数中。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/3
 * github - https://github.com/hl845740757
 */
public interface RpcResponseChannel<T> {

	/**
	 * 返回rpc调用结果，表示调用成功，{@link #write(RpcResponse)}的快捷方式。
	 * @param body rpc调用结果/可能为null
	 */
	default void writeSuccess(@Nullable T body) {
		write(RpcResponse.newSucceedResponse(body));
	}

	/**
	 * 返回rpc调用结果，表示调用失败，{@link #write(RpcResponse)}的快捷方式。
	 * @param errorCode rpc调用错误码，能使用静态常量的使用常量{@link RpcResponse}
	 */
	default void writeFailure(@Nonnull RpcResultCode errorCode) {
		write(RpcResponse.newFailResponse(errorCode));
	}

	/**
	 * 返回rpc调用结果，{@link #write(RpcResponse)}的快捷方式
	 *
	 * @param resultCode rpc调用结果码
	 * @param body 调用结果/额外信息
	 */
	default void write(@Nonnull RpcResultCode resultCode, @Nullable Object body) {
		write(new RpcResponse(resultCode, body));
	}

	/**
	 * 返回rpc调用结果，如果是返回错误结果，能使用静态常量的使用常量{@link RpcResponse}
	 *
	 * @param rpcResponse rpc调用结果
	 */
	void write(@Nonnull RpcResponse rpcResponse);

	/**
	 * 是否不关心结果，true表示不关心结果
	 * @return true/false
	 */
	boolean isVoid();
}
