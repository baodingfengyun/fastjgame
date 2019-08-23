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

package com.wjybxx.fastjgame.mrg;

import com.google.inject.Inject;
import com.wjybxx.fastjgame.core.onlinenode.WarzoneNodeData;
import com.wjybxx.fastjgame.core.onlinenode.WarzoneNodeName;
import com.wjybxx.fastjgame.misc.RpcCall;
import com.wjybxx.fastjgame.misc.SucceedRpcCallback;
import com.wjybxx.fastjgame.misc.WarzoneInCenterInfo;
import com.wjybxx.fastjgame.net.RoleType;
import com.wjybxx.fastjgame.net.Session;
import com.wjybxx.fastjgame.net.SessionLifecycleAware;
import com.wjybxx.fastjgame.rpcproxy.ICenterInWarzoneInfoMrgProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Warzone在Game中的连接管理等控制器
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/15 23:11
 * github - https://github.com/hl845740757
 */
public class WarzoneInCenterInfoMrg {

    private static final Logger logger= LoggerFactory.getLogger(WarzoneInCenterInfoMrg.class);

    private final CenterWorldInfoMrg centerWorldInfoMrg;
    private final InnerAcceptorMrg innerAcceptorMrg;
    /**
     * 连接的战区信息，一定是发现的那个节点的session。
     */
    private WarzoneInCenterInfo warzoneInCenterInfo;

    @Inject
    public WarzoneInCenterInfoMrg(CenterWorldInfoMrg centerWorldInfoMrg, InnerAcceptorMrg innerAcceptorMrg) {
        this.centerWorldInfoMrg = centerWorldInfoMrg;
        this.innerAcceptorMrg = innerAcceptorMrg;
    }

    public WarzoneInCenterInfo getWarzoneInCenterInfo() {
        return warzoneInCenterInfo;
    }

    /**
     * 发现战区出现(zk上出现了该服务器对应的战区节点)
     * @param warzoneNodeName 战区节点名字信息
     * @param warzoneNodeData  战区其它信息
     */
    public void onDiscoverWarzone(WarzoneNodeName warzoneNodeName, WarzoneNodeData warzoneNodeData){
        if (null != warzoneInCenterInfo){
            // 可能丢失了节点消失事件
            logger.error("my loss childRemove event");
            onWarzoneDisconnect(warzoneInCenterInfo.getWarzoneWorldGuid());
        }
        // 注册tcp会话
        innerAcceptorMrg.connect(warzoneNodeData.getWorldGuid(), RoleType.WARZONE,
                warzoneNodeData.getInnerTcpAddress(),
                warzoneNodeData.getLocalAddress(),
                warzoneNodeData.getMacAddress(),
                new WarzoneSessionLifeAware());
    }

    /**
     * 发现战区断开连接(这里现在没有严格的测试，是否可能是不同的节点)
     * @param warzoneNodeName 战区节点名字信息
     */
    public void onWarzoneNodeRemoved(WarzoneNodeName warzoneNodeName, WarzoneNodeData warzoneNodeData){
        onWarzoneDisconnect(warzoneNodeData.getWorldGuid());
    }

    /**
     * 触发的情况有两种:
     * 1.异步会话超时
     * 2.zookeeper节点消息
     *
     * 因为有两种情况，因此后触发的那个是无效的
     * @param worldGuid 战区进程id
     */
    private void onWarzoneDisconnect(long worldGuid){
        if (null == warzoneInCenterInfo){
            return;
        }
        if (warzoneInCenterInfo.getWarzoneWorldGuid() != worldGuid){
            return;
        }
        if (warzoneInCenterInfo.getSession() != null) {
            warzoneInCenterInfo.getSession().close();
        }

        warzoneInCenterInfo = null;
        // TODO 战区宕机需要处理的逻辑
    }

    private class WarzoneSessionLifeAware implements SessionLifecycleAware {

        @Override
        public void onSessionConnected(Session session) {
            ICenterInWarzoneInfoMrgProxy.connectWarzone(centerWorldInfoMrg.getPlatformType().getNumber(), centerWorldInfoMrg.getServerId())
                    .setSession(session)
                    .ifSuccess(result -> connectWarzoneSuccess(session))
                    .execute();;
        }

        @Override
        public void onSessionDisconnected(Session session) {
            onWarzoneDisconnect(session.remoteGuid());
        }
    }

    /**
     * 连接争取安全成功(收到了战区的响应信息)。
     * @param session 与战区的会话
     */
    private void connectWarzoneSuccess(Session session){
        assert null==warzoneInCenterInfo;
        warzoneInCenterInfo = new WarzoneInCenterInfo(session.remoteGuid(), session);

        // TODO 战区连接成功逻辑(eg.恢复特殊玩法)
        logger.info("connect WARZONE-{} success",centerWorldInfoMrg.getWarzoneId());
    }

}
