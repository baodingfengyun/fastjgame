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

import com.wjybxx.fastjgame.annotation.LazySerializable;
import com.wjybxx.fastjgame.annotation.RpcMethod;
import com.wjybxx.fastjgame.annotation.RpcService;

import java.util.List;

/**
 * 网关服玩家session管理器
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/11/12
 * github - https://github.com/hl845740757
 */
@RpcService(serviceId = RpcServiceTable.GATE_PLAYER_SESSION_MGR)
public interface IGatePlayerSessionMgr {

    /**
     * 发送消息给指定玩家
     *
     * @param msg 要发送的消息
     */
    @RpcMethod(methodId = 1)
    void sendToPlayer(long playerGuid, @LazySerializable byte[] msg);

    /**
     * 广播本服所有“在线”玩家
     *
     * @param msg 要广播的消息
     */
    @RpcMethod(methodId = 2)
    void broadcast(@LazySerializable byte[] msg);

    /**
     * 广播消息给指定玩家
     *
     * @param playerGuids 要广播的玩家
     * @param msg         要广播的消息
     */
    @RpcMethod(methodId = 3)
    void broadcast(List<Long> playerGuids, @LazySerializable byte[] msg);
}
