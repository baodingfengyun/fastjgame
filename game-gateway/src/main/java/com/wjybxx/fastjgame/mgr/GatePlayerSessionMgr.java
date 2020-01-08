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

package com.wjybxx.fastjgame.mgr;

import com.google.inject.Inject;
import com.wjybxx.fastjgame.async.GenericFutureResultListener;
import com.wjybxx.fastjgame.misc.GatePlayerSession;
import com.wjybxx.fastjgame.net.common.ProtocolDispatcher;
import com.wjybxx.fastjgame.net.common.RpcFutureResult;
import com.wjybxx.fastjgame.net.common.RpcResponseChannel;
import com.wjybxx.fastjgame.net.session.Session;
import com.wjybxx.fastjgame.rpcservice.IGatePlayerSessionMgr;
import com.wjybxx.fastjgame.rpcservice.IPlayerMessageDispatcherMgrRpcProxy;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * 网关服玩家session管理器
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/11/13
 * github - https://github.com/hl845740757
 */
public class GatePlayerSessionMgr implements IGatePlayerSessionMgr, ProtocolDispatcher {

    private final GateCenterSessionMgr centerSessionMgr;
    private final Long2ObjectMap<GatePlayerSession> sessionMap = new Long2ObjectOpenHashMap<>();

    @Inject
    public GatePlayerSessionMgr(GateCenterSessionMgr centerSessionMgr) {
        this.centerSessionMgr = centerSessionMgr;
    }

    @Override
    public void sendToPlayer(long playerGuid, byte[] msg) {
        final GatePlayerSession playerSession = sessionMap.get(playerGuid);
        if (null != playerSession) {
            playerSession.getPlayerSession().send(msg);
        }
    }

    @Override
    public void broadcast(byte[] msg) {
        sessionMap.values().stream()
                .filter(gatePlayerSession -> gatePlayerSession.getState() == GatePlayerSession.State.LOGIN_SCENE)
                .forEach(playerSession -> {
                    playerSession.getPlayerSession().send(msg);
                });
    }

    @Override
    public void broadcast(List<Long> playerGuids, byte[] msg) {
        for (long playerGuid : playerGuids) {
            sendToPlayer(playerGuid, msg);
        }
    }

    /**
     * 当监听到玩家建立socket连接
     *
     * @param session 玩家的真实连接
     */
    public void onSessionConnected(final Session session) {
        sessionMap.computeIfAbsent(session.remoteGuid(), playerGuid -> new GatePlayerSession(session));
    }

    /**
     * 当监听到玩家的socket断开
     *
     * @param session 玩家的真实连接
     */
    public void onSessionDisconnected(final Session session) {
        final GatePlayerSession playerSession = sessionMap.remove(session.remoteGuid());
        if (null != playerSession) {
            playerSession.setState(GatePlayerSession.State.DISCONNECT);
        }
    }

    @Override
    public void postRpcRequest(Session session, @Nullable Object request, @Nonnull RpcResponseChannel<?> responseChannel) {
        // 玩家不可以向服务器发起rpc请求
        throw new UnsupportedOperationException("rpcRequest " + request);
    }

    @Override
    public <V> void postRpcCallback(Session session, GenericFutureResultListener<RpcFutureResult<V>, ? super V> listener, RpcFutureResult<V> futureResult) {
        // 网关不可以向玩家发送rpc请求
        throw new UnsupportedOperationException("Unexpected rpcCallBack: " + listener.getClass().getName());
    }

    @Override
    public void postOneWayMessage(Session session, @Nullable Object message) {
        final GatePlayerSession playerSession = sessionMap.get(session.remoteGuid());
        if (null == playerSession || playerSession.getPlayerSession() != session) {
            // 非法的session
            session.close();
            return;
        }

        if (message == null) {
            // ignore - 编解码失败
            return;
        }

        // 玩家必须以字节数组格式发送消息 - 这样网关不必进行冗余的编解码操作
        if (!(message instanceof byte[])) {
            session.close();
            return;
        }

        switch (playerSession.getState()) {
            case LOGIN_GATE:
            case LOGIN_CENTER:
                sendToCenter(playerSession, (byte[]) message);
                break;
            case LOGIN_SCENE:
                sendToScene(playerSession, (byte[]) message);
                break;
            case DISCONNECT:
                session.close();
                break;
        }
    }

    private void sendToCenter(GatePlayerSession playerSession, byte[] message) {
        final Session centerSession = centerSessionMgr.getCenterSession();
        if (null == centerSession) {
            playerSession.getPlayerSession().close();
            return;
        }
        IPlayerMessageDispatcherMgrRpcProxy.onPlayerMessage(playerSession.getPlayerGuid(), message)
                .send(centerSession);
    }

    private void sendToScene(GatePlayerSession playerSession, byte[] message) {
        if (playerSession.getSceneSession() == null) {
            playerSession.getPlayerSession().close();
            return;
        }

        IPlayerMessageDispatcherMgrRpcProxy.onPlayerMessage(playerSession.getPlayerGuid(), message)
                .send(playerSession.getSceneSession());
    }


}
