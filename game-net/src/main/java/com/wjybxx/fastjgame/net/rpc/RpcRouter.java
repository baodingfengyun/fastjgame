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

/**
 * rpc路由器
 *
 * @param <V>
 */
@FunctionalInterface
public interface RpcRouter<V> {

    /**
     * 路由实现(将原始请求封装到另一个请求中)
     *
     * @param rpcRequest 原始方法调用信息
     * @return newBuilder，该builder中包含新封装后的调用信息。
     */
    RpcMethodHandle<V> route(RpcRequest<V> rpcRequest);

}
