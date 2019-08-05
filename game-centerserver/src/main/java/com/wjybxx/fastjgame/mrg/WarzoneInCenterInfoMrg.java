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
import com.wjybxx.fastjgame.core.WarzoneInCenterInfo;
import com.wjybxx.fastjgame.core.onlinenode.WarzoneNodeData;
import com.wjybxx.fastjgame.core.onlinenode.WarzoneNodeName;
import com.wjybxx.fastjgame.misc.HostAndPort;
import com.wjybxx.fastjgame.misc.NetContext;
import com.wjybxx.fastjgame.net.C2SSession;
import com.wjybxx.fastjgame.net.RoleType;
import com.wjybxx.fastjgame.net.Session;
import com.wjybxx.fastjgame.net.SessionLifecycleAware;
import com.wjybxx.fastjgame.net.initializer.TCPClientChannelInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.wjybxx.fastjgame.protobuffer.p_center_warzone.p_center_warzone_hello;
import static com.wjybxx.fastjgame.protobuffer.p_center_warzone.p_center_warzone_hello_result;

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
    private final NetContextMrg netContextMrg;
    private final CodecHelperMrg codecHelperMrg;
    private final MessageDispatcherMrg messageDispatcherMrg;

    /**
     * 连接的战区信息，一定是发现的那个节点的session。
     */
    private WarzoneInCenterInfo warzoneInCenterInfo;

    @Inject
    public WarzoneInCenterInfoMrg(CenterWorldInfoMrg centerWorldInfoMrg, NetContextMrg netContextMrg, CodecHelperMrg codecHelperMrg, MessageDispatcherMrg messageDispatcherMrg) {
        this.netContextMrg = netContextMrg;
        this.codecHelperMrg = codecHelperMrg;
        this.centerWorldInfoMrg = centerWorldInfoMrg;
        this.messageDispatcherMrg = messageDispatcherMrg;
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
        // 注册异步tcp会话
        HostAndPort tcpHostAndPort = HostAndPort.parseHostAndPort(warzoneNodeData.getInnerTcpAddress());

        NetContext netContext = netContextMrg.getNetContext();
        TCPClientChannelInitializer tcpClientChannelInitializer = netContext.
                newTcpClientInitializer(warzoneNodeData.getWorldGuid(), codecHelperMrg.getInnerCodecHolder());

        netContext.connect(warzoneNodeData.getWorldGuid(), RoleType.WARZONE, tcpHostAndPort,
                () -> tcpClientChannelInitializer,
                new WarzoneSessionLifeAware(),
                messageDispatcherMrg);
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

    private class WarzoneSessionLifeAware implements SessionLifecycleAware<C2SSession> {

        @Override
        public void onSessionConnected(C2SSession session) {
            p_center_warzone_hello hello = p_center_warzone_hello
                    .newBuilder()
                    .setPlatfomNumber(centerWorldInfoMrg.getPlatformType().getNumber())
                    .setServerId(centerWorldInfoMrg.getServerId())
                    .build();

            session.sendMessage(hello);
        }

        @Override
        public void onSessionDisconnected(C2SSession session) {
            onWarzoneDisconnect(session.getServerGuid());
        }
    }

    /**
     * 收到了战区的响应信息。
     * @param session 与战区的会话
     * @param result 战区返回的信息
     */
    public void p_center_warzone_hello_result_handler(Session session, p_center_warzone_hello_result result){
        assert null==warzoneInCenterInfo;
        warzoneInCenterInfo = new WarzoneInCenterInfo(session.remoteGuid(), session);

        // TODO 战区连接成功逻辑(eg.恢复特殊玩法)
        logger.info("connect WARZONE-{} success",centerWorldInfoMrg.getWarzoneId());
    }

}
