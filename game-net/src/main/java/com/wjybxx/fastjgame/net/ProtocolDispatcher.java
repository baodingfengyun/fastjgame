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

import javax.annotation.Nullable;

/**
 * 协议分发器，将。
 * {@link ProtocolCodec}在网络层，而{@link ProtocolDispatcher}在应用层，在用户线程。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/4/27 22:05
 * github - https://github.com/hl845740757
 */
public interface ProtocolDispatcher {

    /**
     * 处理该会话发来的Rpc请求
     * @param session 会话信息
     * @param request rpc请求，如果编解码异常，则可能为null。
     * @param context 该rpc请求的特定上下文，可以用于创建返回结果的channel {@link Session#newResponseChannel(RpcRequestContext)}。
     *                注意：该context不可以共享，不可以用在其它请求上。
     * @throws Exception error
     */
    void dispatchRpcRequest(Session session, @Nullable Object request, RpcRequestContext context) throws Exception;

    /**
     * 处理该会话发来单向的消息
     * @param session 会话信息
     * @param message 业务逻辑消息，如果编解码异常，则可能为null。
     * @throws Exception error
     */
    void dispatchOneWayMessage(Session session, @Nullable Object message) throws Exception;

}
