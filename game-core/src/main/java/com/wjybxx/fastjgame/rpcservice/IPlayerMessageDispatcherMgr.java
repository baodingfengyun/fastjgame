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

import com.wjybxx.fastjgame.annotation.PreDeserializable;
import com.wjybxx.fastjgame.annotation.RpcMethod;
import com.wjybxx.fastjgame.annotation.RpcService;
import com.wjybxx.fastjgame.net.session.Session;

import javax.annotation.Nullable;

/**
 * 玩家消息处理器(网关通过这种方式转发消息)
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/10/10
 * github - https://github.com/hl845740757
 */
@RpcService(serviceId = RpcServiceTable.PLAYER_MESSAGE_DISPATCHER_MGR)
public interface IPlayerMessageDispatcherMgr {

    /**
     * 接收到一个网关转发过来的玩家消息
     *
     * @param session    网关session
     * @param playerGuid 玩家guid
     * @param message    玩家发来的消息，使用{@link PreDeserializable}注解，网络层可以提前反序列化
     */
    @RpcMethod(methodId = 1)
    void onPlayerMessage(Session session, long playerGuid, @Nullable @PreDeserializable Object message);

}
