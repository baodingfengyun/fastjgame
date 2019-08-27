/*
 *  Copyright 2019 wjybxx
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to iBn writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.wjybxx.fastjgame.net;

import javax.annotation.Nonnull;

/**
 * http请求处理器注册表
 * @author houlei
 * @version 1.0
 * date - 2019/8/27
 */
public interface HttpRequestHandlerRegistry {

	/**
	 * 注册一个路径的请求处理
	 * @param path 路径
	 * @param httpRequestHandler 请求处理器
	 */
	void register(@Nonnull String path, @Nonnull HttpRequestHandler httpRequestHandler);

}
