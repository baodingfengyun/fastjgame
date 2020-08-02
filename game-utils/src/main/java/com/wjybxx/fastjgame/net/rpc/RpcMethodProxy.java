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

package com.wjybxx.fastjgame.net.rpc;

import com.wjybxx.fastjgame.net.session.Session;
import com.wjybxx.fastjgame.util.concurrent.Promise;

import java.util.List;

/**
 * rpc方法代理。
 * 用于代码生成工具为{@link RpcMethod}生成对应lambda表达式，以代替反射调用。
 * 当然也可以手写实现。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/20
 * github - https://github.com/hl845740757
 */
@FunctionalInterface
public interface RpcMethodProxy {

    /**
     * 执行调用
     *
     * @param context      rpc执行时的一些上下文
     * @param methodParams 对应的方法参数，发过来的参数不包含{@link Session} 和 {@link Promise}。
     *                     如果原始方法需要的话，代理方法需要自动传入。
     */
    Object invoke(RpcProcessContext context, List<Object> methodParams) throws Exception;

}
