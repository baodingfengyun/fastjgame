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
public final class ServiceTable {

    /**
     * 中心服在战区服的信息管理
     */
    public static final short CENTER_IN_WARZONE_INFO_MGR = 1;
    /**
     * 中心服在场景服的信息管理
     */
    public static final short CENTER_IN_SCENE_INFO_MGR = 2;
    /**
     * 场景服区域管理器
     */
    public static final short SCENE_REGION_MGR = 3;
    /**
     * 玩家协议处理器
     */
    public static final short PLAYER_MESSAGE_DISPATCHER_MGR = 4;
}
