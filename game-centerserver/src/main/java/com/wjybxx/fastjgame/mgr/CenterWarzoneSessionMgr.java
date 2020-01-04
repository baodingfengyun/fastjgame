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

package com.wjybxx.fastjgame.mgr;

import com.google.inject.Inject;
import com.wjybxx.fastjgame.misc.CenterWarzoneSession;
import com.wjybxx.fastjgame.net.common.SessionLifecycleAware;
import com.wjybxx.fastjgame.net.session.Session;
import com.wjybxx.fastjgame.node.WarzoneNodeData;
import com.wjybxx.fastjgame.node.WarzoneNodeName;
import com.wjybxx.fastjgame.rpcservice.IWarzoneCenterSessionMgrRpcProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;


/**
 * Warzone在Game中的连接管理等控制器
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/15 23:11
 * github - https://github.com/hl845740757
 */
public class CenterWarzoneSessionMgr {

    private static final Logger logger = LoggerFactory.getLogger(CenterWarzoneSessionMgr.class);

    private final CenterWorldInfoMgr centerWorldInfoMgr;
    private final GameAcceptorMgr gameAcceptorMgr;
    /**
     * 连接的战区信息，一定是发现的那个节点的session。
     */
    private CenterWarzoneSession warzoneSession;

    @Inject
    public CenterWarzoneSessionMgr(CenterWorldInfoMgr centerWorldInfoMgr, GameAcceptorMgr gameAcceptorMgr) {
        this.centerWorldInfoMgr = centerWorldInfoMgr;
        this.gameAcceptorMgr = gameAcceptorMgr;
    }

    @Nullable
    public CenterWarzoneSession getWarzoneInfo() {
        return warzoneSession;
    }

    @Nullable
    public Session getWarzoneSession() {
        return warzoneSession == null ? null : warzoneSession.getSession();
    }

    /**
     * 发现战区出现(zk上出现了该服务器对应的战区节点)
     *
     * @param warzoneNodeName 战区节点名字信息
     * @param warzoneNodeData 战区其它信息
     */
    public void onDiscoverWarzone(WarzoneNodeName warzoneNodeName, WarzoneNodeData warzoneNodeData) {
        if (null != warzoneSession) {
            // 可能丢失了节点消失事件
            logger.error("my loss childRemove event");
            onWarzoneDisconnect(warzoneSession.getWarzoneWorldGuid());
        }
        // 注册tcp会话
        gameAcceptorMgr.connect(warzoneNodeData.getWorldGuid(),
                warzoneNodeData.getInnerTcpAddress(),
                new WarzoneSessionLifeAware());
    }

    private class WarzoneSessionLifeAware implements SessionLifecycleAware {

        @Override
        public void onSessionConnected(Session session) {
            IWarzoneCenterSessionMgrRpcProxy.register(centerWorldInfoMgr.getServerId())
                    .onSuccess(result -> onRegisterWarzoneResult(session, result))
                    .onFailure(rpcResponse -> session.close())
                    .call(session);
        }

        @Override
        public void onSessionDisconnected(Session session) {
            onWarzoneDisconnect(session.remoteGuid());
        }
    }

    /**
     * 连接争取安全成功(收到了战区的响应信息)。
     *
     * @param session 与战区的会话
     * @param result  建立连接是否成功
     */
    private void onRegisterWarzoneResult(Session session, boolean result) {
        if (!result) {
            // 连接失败
            session.close();
            return;
        }

        if (warzoneSession != null) {
            session.close();
            logger.error("connect two warzone session.");
            return;
        }
        warzoneSession = new CenterWarzoneSession(session);

        // TODO 战区连接成功逻辑(eg.恢复特殊玩法)
        logger.info("connect WARZONE-{} success", centerWorldInfoMgr.getWarzoneId());
    }


    /**
     * 发现战区断开连接(这里现在没有严格的测试，是否可能是不同的节点)
     *
     * @param warzoneNodeName 战区节点名字信息
     */
    public void onWarzoneNodeRemoved(WarzoneNodeName warzoneNodeName, WarzoneNodeData warzoneNodeData) {
        onWarzoneDisconnect(warzoneNodeData.getWorldGuid());
    }

    /**
     * 触发的情况有两种:
     * 1.异步会话超时
     * 2.zookeeper节点消息
     * <p>
     * 因为有两种情况，因此后触发的那个是无效的
     *
     * @param worldGuid 战区进程id
     */
    private void onWarzoneDisconnect(long worldGuid) {
        if (null == warzoneSession) {
            return;
        }
        if (warzoneSession.getWarzoneWorldGuid() != worldGuid) {
            return;
        }
        if (warzoneSession.getSession() != null) {
            warzoneSession.getSession().close();
        }

        warzoneSession = null;
        // TODO 战区宕机需要处理的逻辑
        logger.warn("WARZONE-{} disconnect", centerWorldInfoMgr.getWarzoneId());
    }

}
