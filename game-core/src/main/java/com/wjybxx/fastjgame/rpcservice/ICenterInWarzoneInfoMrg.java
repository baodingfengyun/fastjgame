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
import com.wjybxx.fastjgame.net.session.Session;

/**
 * 中心服在战区服的信息管理器
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/22
 * github - https://github.com/hl845740757
 */
@RpcService(serviceId = ServiceTable.CENTER_IN_WARZONE_INFO_MRG)
public interface ICenterInWarzoneInfoMrg {

    /**
     * 中心服请求注册到战区服
     *
     * @param session       关联的会话
     * @param platfomNumber 中心服的平台
     * @param serverId      中心服的服ID
     * @return 返回一个结果告知已完成
     */
    @RpcMethod(methodId = 1)
    boolean connectWarzone(Session session, int platfomNumber, int serverId);
}
