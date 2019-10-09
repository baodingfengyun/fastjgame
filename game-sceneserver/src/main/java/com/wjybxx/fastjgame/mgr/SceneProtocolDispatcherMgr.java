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
import com.wjybxx.fastjgame.misc.DefaultPlayerMessageDispatcher;
import com.wjybxx.fastjgame.misc.PlayerMessageFunction;
import com.wjybxx.fastjgame.misc.PlayerMessageFunctionRegistry;
import com.wjybxx.fastjgame.net.session.Session;

import javax.annotation.Nonnull;

/**
 * 场景服派发协议的实现
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/26
 */
public class SceneProtocolDispatcherMgr extends ProtocolDispatcherMgr implements PlayerMessageFunctionRegistry {

    private final PlayerSessionMgr playerSessionMgr;
    private final DefaultPlayerMessageDispatcher messageDispatcher = new DefaultPlayerMessageDispatcher();

    @Inject
    public SceneProtocolDispatcherMgr(PlayerSessionMgr playerSessionMgr) {
        this.playerSessionMgr = playerSessionMgr;
    }

    @Override
    protected final void dispatchOneWayMessage(Session session, @Nonnull Object message) {
        // TODO 判断是否是玩家
        if (message instanceof AbstractMessage) {
            Player player = playerSessionMgr.getPlayer(session.remoteGuid());
            if (player != null) {
                // 玩家已成功连入场景
                messageDispatcher.post(player, (AbstractMessage) message);
            } else {
                // TODO 判断是玩家登录消息
            }
        }
    }

    @Override
    public <T extends AbstractMessage> void register(@Nonnull Class<T> clazz, @Nonnull PlayerMessageFunction<T> handler) {
        messageDispatcher.register(clazz, handler);
    }

    @Override
    public void release() {
        super.release();
        messageDispatcher.release();
    }
}
