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

package com.wjybxx.fastjgame.annotation;

import com.wjybxx.fastjgame.net.HttpRequestHandler;
import com.wjybxx.fastjgame.net.HttpRequestParam;
import com.wjybxx.fastjgame.net.HttpSession;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * http请求路由。
 * 该注解可以用在类/接口上，也可以用在方法上。当类上存在该注解时，那么该类中的所有方法默认都在该路径之下。
 *
 * 方法必须满足以下要求，否则编译会报错：
 * <li>1. 必须是3个参数：第一个必须{@link HttpSession}，第二个参数为{@link String}，第三个参数必须是{@link HttpRequestParam}。
 * 也就是可以转换为{@link HttpRequestHandler}</li>
 * <li>2. 必须是public </li>
 *
 * eg:
 * <pre>{@code
 * 		@HttpRequestMapping
 * 		public void onRequest(HttpSession session, String path, HttpRequestParam param) {
 * 			// do something
 * 		}
 * }
 * </pre>
 *
 * 没想到啥好名字，参考下了常见的spring的RequestMapping，也打算做个类似的支持。
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/27
 * github - https://github.com/hl845740757
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface HttpRequestMapping {

	/**
	 * 要监听的http请求路径。
	 * 路径必须以'/'开头，eg: /main
	 *
	 * @return path
	 */
	String path();

	/**
	 * 是否继承父节点的路径，默认支持.。
	 * 如果不继承父节点路径，则使用指定的路径，否则使用父节点路径和当前路径拼接完的路径。
	 *
	 * @return true/false
	 */
	boolean inherit() default true;
}
