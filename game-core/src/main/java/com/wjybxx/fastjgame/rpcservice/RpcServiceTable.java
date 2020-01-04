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

/**
 * 所有的rpcServiceId,方便管理
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/20
 * github - https://github.com/hl845740757
 */
public final class RpcServiceTable {

    /**
     * 战区服测试组件
     */
    public static final short WARZONE_TEST = 32001;
    /**
     * 场景服测试组件
     */
    public static final short SCENE_TEST = 32002;

    /**
     * 中心服在战区服的信息管理
     */
    public static final short WARZONE_CENTER_SESSION_MGR = 1;

    /**
     * 中心服在场景服的信息管理
     */
    public static final short SCENE_CENTER_SESSION_MGR = 2;

    /**
     * 网关服在场景服的信息管理
     */
    public static final short SCENE_GATE_SESSION_MGR = 3;

    /**
     * 场景服区域管理器
     */
    public static final short SCENE_REGION_MGR = 4;

    /**
     * 玩家协议处理器
     */
    public static final short PLAYER_MESSAGE_DISPATCHER_MGR = 5;

    /**
     * 网关服在中心服的信息管理
     */
    public static final short CENTER_GATE_SESSION_MGR = 6;

    /**
     * 玩家在网关服的会话管理器
     */
    public static final short GATE_PLAYER_SESSION_MGR = 7;

    /**
     * 中心服路由管理器(负责 scene到warzone的转发 和 warzone到scene的转发)
     */
    public static final short CENTER_ROUTER_MGR = 8;
}
