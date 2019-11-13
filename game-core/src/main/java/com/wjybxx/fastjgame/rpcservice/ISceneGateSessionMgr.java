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
import com.wjybxx.fastjgame.misc.CenterServerId;
import com.wjybxx.fastjgame.net.session.Session;

/**
 * 网关服在场景服的信息管理
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/11/3
 * github - https://github.com/hl845740757
 */
@RpcService(serviceId = RpcServiceTable.SCENE_GATE_SESSION_MGR)
public interface ISceneGateSessionMgr {

    /**
     * 网关服请求注册到场景服上
     *
     * @param session  网关服session
     * @param serverId 网关服所属的中心服Id
     * @return 注册是否成功
     */
    @RpcMethod(methodId = 1)
    boolean register(Session session, CenterServerId serverId);
}
