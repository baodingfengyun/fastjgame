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

package com.wjybxx.fastjgame.net.misc;

import com.wjybxx.fastjgame.net.annotation.RpcMethod;
import com.wjybxx.fastjgame.net.common.RpcResponseChannel;
import com.wjybxx.fastjgame.net.session.Session;

import java.util.List;

/**
 * rpc请求处理函数。
 * 用于代码生成工具为{@link RpcMethod}生成对应lambda表达式，以代替反射调用。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/20
 * github - https://github.com/hl845740757
 */
@FunctionalInterface
public interface RpcFunction<T> {

    /**
     * 执行调用
     *
     * @param session         请求方对应的session
     * @param methodParams    对应的方法参数，发过来的参数不包含{@link Session} 和 {@link RpcResponseChannel}，如果需要的话，代理方法需要完成该处理。
     * @param responseChannel 返回结果的通道
     */
    void call(Session session, List<Object> methodParams, RpcResponseChannel<T> responseChannel) throws Exception;

}
