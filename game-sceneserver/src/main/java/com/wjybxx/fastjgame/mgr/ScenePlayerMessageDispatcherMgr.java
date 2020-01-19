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
import com.google.protobuf.Message;
import com.wjybxx.fastjgame.eventbus.EventHandler;
import com.wjybxx.fastjgame.eventbus.EventHandlerRegistry;
import com.wjybxx.fastjgame.eventbus.GenericEvent;
import com.wjybxx.fastjgame.gameobject.Player;
import com.wjybxx.fastjgame.misc.PlayerMsgEvent;
import com.wjybxx.fastjgame.net.session.Session;
import com.wjybxx.fastjgame.rpcservice.IPlayerMessageDispatcherMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * 网关服转发玩家消息给场景服
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/26
 */
public class ScenePlayerMessageDispatcherMgr implements EventHandlerRegistry, IPlayerMessageDispatcherMgr {

    private static final Logger logger = LoggerFactory.getLogger(ScenePlayerMessageDispatcherMgr.class);

    private final PlayerSessionMgr playerSessionMgr;
    private final Map<Class<?>, EventHandler<? super PlayerMsgEvent<?>>> handlerMap = new IdentityHashMap<>(1024);

    @Inject
    public ScenePlayerMessageDispatcherMgr(PlayerSessionMgr playerSessionMgr) {
        this.playerSessionMgr = playerSessionMgr;
    }

    @Override
    public void onPlayerMessage(Session session, long playerGuid, @Nullable Object message) {
        if (null == message) {
            // 发序列化错误
            logger.warn("gateway {} - player {} send null message", session.sessionId(), playerGuid);
            return;
        }

        final EventHandler<? super PlayerMsgEvent<?>> msgHandler = handlerMap.get(message.getClass());
        if (msgHandler == null) {
            logger.warn("gateway {} - player {} send unregistered message", session.sessionId(), playerGuid);
            return;
        }

        final Player player = playerSessionMgr.getPlayer(playerGuid);
        if (null != player) {
            final PlayerMsgEvent<Message> playerMsgEvent = new PlayerMsgEvent<>(player, (Message) message);
            try {
                msgHandler.onEvent(playerMsgEvent);
            } catch (Exception e) {
                logger.warn(" msgHandler.onEvent caught exception, sessionId = {}, playerGuid = {}, msg = {}",
                        session.sessionId(), playerGuid, message);
            }
        }
        // else 玩家不在当前场景world
    }

    @Override
    public <T> void register(@Nonnull Class<T> eventType, @Nonnull EventHandler<? super T> handler) {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends GenericEvent<U>, U> void register(@Nonnull Class<T> genericType, Class<U> childType, @Nonnull EventHandler<? super T> handler) {
        if (!PlayerMsgEvent.class.isAssignableFrom(genericType)) {
            return;
        }
        // 只注册消息处理器
        handlerMap.put(childType, (EventHandler<? super PlayerMsgEvent>) handler);
    }

    @Override
    public void release() {
        handlerMap.clear();
    }
}
