package com.wjybxx.fastjgame.world;

import com.google.inject.Inject;
import com.wjybxx.fastjgame.mgr.*;
import com.wjybxx.fastjgame.misc.HostAndPort;
import com.wjybxx.fastjgame.net.common.SessionLifecycleAware;
import com.wjybxx.fastjgame.net.session.Session;
import com.wjybxx.fastjgame.node.SceneNodeData;
import com.wjybxx.fastjgame.rpcservice.IPlayerMessageDispatcherMgrRpcRegister;
import com.wjybxx.fastjgame.rpcservice.ISceneCenterSessionMgrRpcRegister;
import com.wjybxx.fastjgame.rpcservice.ISceneGateSessionMgrRpcRegister;
import com.wjybxx.fastjgame.rpcservice.ISceneRegionMgrRpcRegister;
import com.wjybxx.fastjgame.utils.JsonUtils;
import com.wjybxx.fastjgame.utils.ZKPathUtils;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.CreateMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * SceneServer
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/15 21:45
 * github - https://github.com/hl845740757
 */
public class SceneWorld extends AbstractWorld {

    private static final Logger logger = LoggerFactory.getLogger(SceneWorld.class);

    private final SceneCenterSessionMgr sceneCenterSessionMgr;
    private final SceneGateSessionMgr sceneGateSessionMgr;
    private final SceneRegionMgr sceneRegionMgr;
    private final SceneWorldInfoMgr sceneWorldInfoMgr;
    private final SceneSendMgr sendMgr;
    private final SceneMgr sceneMgr;
    private final ScenePlayerMessageDispatcherMgr playerMessageDispatcherMgr;

    @Inject
    public SceneWorld(WorldWrapper worldWrapper, SceneCenterSessionMgr sceneCenterSessionMgr,
                      SceneGateSessionMgr sceneGateSessionMgr, SceneRegionMgr sceneRegionMgr,
                      SceneSendMgr sendMgr, SceneMgr sceneMgr,
                      ScenePlayerMessageDispatcherMgr playerMessageDispatcherMgr) {
        super(worldWrapper);
        this.sceneCenterSessionMgr = sceneCenterSessionMgr;
        this.sceneGateSessionMgr = sceneGateSessionMgr;
        this.sceneRegionMgr = sceneRegionMgr;
        this.sceneWorldInfoMgr = (SceneWorldInfoMgr) worldWrapper.getWorldInfoMgr();
        this.sendMgr = sendMgr;
        this.sceneMgr = sceneMgr;
        this.playerMessageDispatcherMgr = playerMessageDispatcherMgr;
    }

    @Override
    protected void registerProtocolCodecs() throws Exception {
        super.registerProtocolCodecs();
        // 这里没有使用模板方法是因为不是都有额外的codec要注册，导致太多钩子方法也不好
        // TODO 注册与玩家交互的协议编解码器
    }

    @Override
    protected void registerRpcService() {
        // 也可以在管理器里进行注册
        ISceneRegionMgrRpcRegister.register(protocolDispatcherMgr, sceneRegionMgr);
        ISceneCenterSessionMgrRpcRegister.register(protocolDispatcherMgr, sceneCenterSessionMgr);
        ISceneGateSessionMgrRpcRegister.register(protocolDispatcherMgr, sceneGateSessionMgr);
        IPlayerMessageDispatcherMgrRpcRegister.register(protocolDispatcherMgr, playerMessageDispatcherMgr);
    }

    @Override
    protected void registerHttpRequestHandlers() {

    }

    @Override
    protected void registerEventHandlers() {

    }

    @Override
    protected void startHook() throws Exception {
        // 启动场景
        sceneRegionMgr.onWorldStart();
        // 注册到zookeeper
        bindAndRegisterToZK();
    }

    private void bindAndRegisterToZK() throws Exception {
        final CenterOrGateLifeAware centerOrGateLifeAware = new CenterOrGateLifeAware();
        // 绑定JVM内部端口
        gameAcceptorMgr.bindLocalPort(centerOrGateLifeAware);
        // 绑定socket端口
        HostAndPort innerTcpAddress = gameAcceptorMgr.bindInnerTcpPort(centerOrGateLifeAware);
        HostAndPort innerHttpAddress = gameAcceptorMgr.bindInnerHttpPort();

        // 节点数据
        final String nodeName = ZKPathUtils.buildSceneNodeName(sceneWorldInfoMgr.getWorldGuid());
        final SceneNodeData sceneNodeData = new SceneNodeData(innerHttpAddress.toString(),
                innerTcpAddress.toString());

        final String parentPath = ZKPathUtils.onlineWarzonePath(sceneWorldInfoMgr.getWarzoneId());
        curatorMgr.createNode(ZKPaths.makePath(parentPath, nodeName), CreateMode.EPHEMERAL, JsonUtils.toJsonBytes(sceneNodeData));
    }

    @Override
    protected void tickHook() {
        sceneMgr.tick();
    }

    @Override
    protected void shutdownHook() {
        playerMessageDispatcherMgr.release();
    }

    private class CenterOrGateLifeAware implements SessionLifecycleAware {

        @Override
        public void onSessionConnected(Session session) {

        }

        @Override
        public void onSessionDisconnected(Session session) {
            final Session centerSession = sceneCenterSessionMgr.getCenterSession(session.remoteGuid());
            if (null != centerSession) {
                // 中心服
                sceneCenterSessionMgr.onCenterDisconnect(session.remoteGuid());
                return;
            }

            final Session gateSession = sceneGateSessionMgr.getGateSession(session.remoteGuid());
            if (null != gateSession) {
                // 网关服
                sceneGateSessionMgr.onSessionDisconnect(session.remoteGuid());
            }

        }
    }

}
