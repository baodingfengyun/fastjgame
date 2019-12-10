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
import com.google.protobuf.AbstractMessage;
import com.wjybxx.fastjgame.gameobject.Player;
import com.wjybxx.fastjgame.misc.DefaultPlayerEventDispatcher;
import com.wjybxx.fastjgame.net.session.Session;
import com.wjybxx.fastjgame.rpcservice.IPlayerMessageDispatcherMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

/**
 * 网关服转发玩家消息给场景服
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/26
 */
public class PlayerEventDispatcherMgr extends DefaultPlayerEventDispatcher implements IPlayerMessageDispatcherMgr {

    private static final Logger logger = LoggerFactory.getLogger(PlayerEventDispatcherMgr.class);

    private final PlayerSessionMgr playerSessionMgr;

    @Inject
    public PlayerEventDispatcherMgr(PlayerSessionMgr playerSessionMgr) {
        this.playerSessionMgr = playerSessionMgr;
    }

    @Override
    public void onPlayerMessage(Session session, long playerGuid, @Nullable AbstractMessage message) {
        if (null == message) {
            // 发序列化错误
            logger.warn("gateway {} - player {} send null message", session.sessionId(), playerGuid);
            return;
        }
        final Player player = playerSessionMgr.getPlayer(playerGuid);
        if (null != player) {
            post(player, message);
        }
        // else 玩家不在当前场景world
    }

}
