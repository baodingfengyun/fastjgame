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

import javax.annotation.Nonnull;

/**
 * Rpc调用函数注册表，本质是发布订阅/观察者的一种
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/21
 * github - https://github.com/hl845740757
 */
public interface RpcFunctionRegistry {

	/**
	 * 注册一个rpc请求处理函数
	 * @param methodKey 方法唯一键
	 * @param function 处理函数，该函数由代理代码生成工具自动生成，当然你也可以闲得蛋疼自己写。
	 */
	void register(int methodKey, @Nonnull RpcFunction function);

	/**
	 * 释放所有的资源，因为{@link #register(int, RpcFunction)}会捕获太多对象，当不再使用{@link RpcFunctionRegistry}时，
	 * 手动的释放，避免因为registry对象存在导致内存泄漏。
	 */
	void release();
}
