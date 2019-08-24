package com.wjybxx.fastjgame.world;

import com.google.inject.Inject;
import com.wjybxx.fastjgame.core.SceneWorldType;
import com.wjybxx.fastjgame.core.onlinenode.SceneNodeData;
import com.wjybxx.fastjgame.misc.HostAndPort;
import com.wjybxx.fastjgame.mrg.*;
import com.wjybxx.fastjgame.net.NetContext;
import com.wjybxx.fastjgame.net.Session;
import com.wjybxx.fastjgame.net.SessionLifecycleAware;
import com.wjybxx.fastjgame.net.initializer.TCPServerChannelInitializer;
import com.wjybxx.fastjgame.net.initializer.WsServerChannelInitializer;
import com.wjybxx.fastjgame.rpcregister.ICenterInSceneInfoMrgRpcRegister;
import com.wjybxx.fastjgame.rpcregister.ISceneRegionMrgRpcRegister;
import com.wjybxx.fastjgame.utils.*;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.CreateMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * SceneServer
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/15 21:45
 * github - https://github.com/hl845740757
 */
public class SceneWorld extends AbstractWorld {

    private static final Logger logger = LoggerFactory.getLogger(SceneWorld.class);

    private final CenterInSceneInfoMrg centerInSceneInfoMrg;
    private final SceneRegionMrg sceneRegionMrg;
    private final SceneWorldInfoMrg sceneWorldInfoMrg;
    private final SceneSendMrg sendMrg;
    private final SceneMrg sceneMrg;

    @Inject
    public SceneWorld(WorldWrapper worldWrapper, CenterInSceneInfoMrg centerInSceneInfoMrg, SceneRegionMrg sceneRegionMrg,
                      SceneSendMrg sendMrg, SceneMrg sceneMrg) {
        super(worldWrapper);
        this.centerInSceneInfoMrg = centerInSceneInfoMrg;
        this.sceneRegionMrg = sceneRegionMrg;
        this.sceneWorldInfoMrg= (SceneWorldInfoMrg) worldWrapper.getWorldInfoMrg();
        this.sendMrg = sendMrg;
        this.sceneMrg = sceneMrg;
    }

    @Override
    protected void registerProtocolCodecs() throws Exception {
        super.registerProtocolCodecs();
        // 这里没有使用模板方法是因为不是都有额外的codec要注册，导致太多钩子方法也不好
        // TODO 注册与玩家交互的codec帮助类
    }

    @Override
    protected void registerMessageHandlers() {

    }

    @Override
    protected void registerRpcService() {
        // 也可以在管理器里进行注册
        ISceneRegionMrgRpcRegister.register(messageDispatcherMrg, sceneRegionMrg);
        ICenterInSceneInfoMrgRpcRegister.register(messageDispatcherMrg, centerInSceneInfoMrg);
    }

    @Override
    protected void registerHttpRequestHandlers() {

    }

    @Override
    protected void startHook() throws Exception {
        // 启动场景
        sceneRegionMrg.onWorldStart();
        // 注册到zookeeper
        bindAndRegisterToZK();

    }

    private void bindAndRegisterToZK() throws Exception {
        // 绑定3个内部交互的端口
        HostAndPort innerTcpAddress = innerAcceptorMrg.bindInnerTcpPort(new CenterLifeAware());
        HostAndPort innerHttpAddress = innerAcceptorMrg.bindInnerHttpPort();
        HostAndPort localAddress = innerAcceptorMrg.bindLocalTcpPort(new CenterLifeAware());

        // 绑定与玩家交互的两个端口
        // TODO 这里需要和前端确定到底使用什么通信方式，暂时使用服务器之间机制
        NetContext netContext = netContextMrg.getNetContext();
        TCPServerChannelInitializer tcplInitializer = netContext.newTcpServerInitializer(protocolCodecMrg.getInnerProtocolCodec());

        HostAndPort outerTcpHostAndPort = netContext.bindRange(NetUtils.getOuterIp(), GameUtils.OUTER_TCP_PORT_RANGE,
                tcplInitializer, new PlayerLifeAware(), messageDispatcherMrg).get();

        WsServerChannelInitializer wsInitializer = netContext.newWsServerInitializer("/ws", protocolCodecMrg.getInnerProtocolCodec());
        HostAndPort outerWebsocketHostAndPort = netContext.bindRange(NetUtils.getOuterIp(), GameUtils.OUTER_WS_PORT_RANGE,
                wsInitializer, new PlayerLifeAware(), messageDispatcherMrg).get();

        SceneNodeData sceneNodeData =new SceneNodeData(innerTcpAddress.toString(), innerHttpAddress.toString(), localAddress.toString(), SystemUtils.getMAC(),
                sceneWorldInfoMrg.getChannelId(), outerTcpHostAndPort.toString(),outerWebsocketHostAndPort.toString());

        String parentPath= ZKPathUtils.onlineParentPath(sceneWorldInfoMrg.getWarzoneId());
        String nodeName;
        if (sceneWorldInfoMrg.getSceneWorldType()== SceneWorldType.SINGLE){
            nodeName= ZKPathUtils.buildSingleSceneNodeName(sceneWorldInfoMrg.getPlatformType(),sceneWorldInfoMrg.getServerId(),sceneWorldInfoMrg.getWorldGuid());
        }else {
            nodeName= ZKPathUtils.buildCrossSceneNodeName(sceneWorldInfoMrg.getWorldGuid());
        }
        curatorMrg.createNode(ZKPaths.makePath(parentPath,nodeName), CreateMode.EPHEMERAL, JsonUtils.toJsonBytes(sceneNodeData));
    }

    @Override
    protected void tickHook() {
        sceneMrg.tick();
    }

    @Override
    protected void shutdownHook() {

    }

    private class CenterLifeAware implements SessionLifecycleAware {
        @Override
        public void onSessionConnected(Session session) {

        }

        @Override
        public void onSessionDisconnected(Session session) {
            centerInSceneInfoMrg.onDisconnect(session.remoteGuid(), SceneWorld.this);
        }
    }

    private class PlayerLifeAware implements SessionLifecycleAware {

        @Override
        public void onSessionConnected(Session session) {

        }

        @Override
        public void onSessionDisconnected(Session session) {

        }
    }
}
