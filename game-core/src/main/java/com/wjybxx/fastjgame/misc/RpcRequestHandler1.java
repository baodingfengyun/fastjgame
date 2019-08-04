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

package com.wjybxx.fastjgame.misc;

import com.wjybxx.fastjgame.net.Session;

/**
 * Rpc请求处理器1类型，可以直接返回结果的方法。
 * (1类型rpc处理器，立即返回结果)
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/4
 * github - https://github.com/hl845740757
 */
@FunctionalInterface
public interface RpcRequestHandler1<T,R> {

    /**
     * 当接收到一个rpc请求时
     * @param session 发送方的信息
     * @param request rpc请求内容
     * @return 返回的结果类型，会自动返回给远程
     */
    R onRequest(Session session, T request);
}
