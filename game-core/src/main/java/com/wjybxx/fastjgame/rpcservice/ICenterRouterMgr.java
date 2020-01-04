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

package com.wjybxx.fastjgame.rpcservice;

import com.wjybxx.fastjgame.annotation.RpcMethod;
import com.wjybxx.fastjgame.annotation.RpcService;
import com.wjybxx.fastjgame.misc.RpcCall;
import com.wjybxx.fastjgame.net.common.RpcResponseChannel;

/**
 * 中心服路由管理器。
 * 负责转发：① scene到warzone的rpc调用 ② warzone到scene服的rpc调用
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/1/4
 * github - https://github.com/hl845740757
 */
@RpcService(serviceId = RpcServiceTable.CENTER_ROUTER_MGR)
public interface ICenterRouterMgr {

    /**
     * 请求调用战区服务器的方法。
     * 由于warzone是确定的，因此不需要额外的参数确定战区服务器的session。
     *
     * @param rpcCall            战区服务器的某个rpc方法信息
     * @param rpcResponseChannel 用于向scene返回调用结果
     */
    @RpcMethod(methodId = 1)
    void sendToWarzone(RpcCall rpcCall, RpcResponseChannel<?> rpcResponseChannel);

    /**
     * 请求发送消息到某个指定scene服务器
     *
     * @param sceneWorldGuid     场景服务器id
     * @param rpcCall            对应的场景服务器rpc方法信息
     * @param rpcResponseChannel 用于向战区服务器返回调用结果
     */
    @RpcMethod(methodId = 2)
    void sendToScene(long sceneWorldGuid, RpcCall rpcCall, RpcResponseChannel<?> rpcResponseChannel);

    /**
     * 请求发送消息到玩家所在场景服务器
     *
     * @param playerGuid         玩家id，用于查找玩家所在场景服务器id。
     * @param rpcCall            对应的场景服务器rpc方法信息。
     * @param rpcResponseChannel 用于向战区服务器返回调用结果
     */
    @RpcMethod(methodId = 3)
    void sendToPlayerScene(long playerGuid, RpcCall rpcCall, RpcResponseChannel<?> rpcResponseChannel);
}
