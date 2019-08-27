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

package com.wjybxx.fastjgame.mrg;

import com.google.inject.Inject;
import com.google.protobuf.AbstractMessage;
import com.wjybxx.fastjgame.gameobject.Player;
import com.wjybxx.fastjgame.misc.DefaultPlayerMessageDispatcher;
import com.wjybxx.fastjgame.misc.PlayerMessageDispatcher;
import com.wjybxx.fastjgame.misc.PlayerMessageFunction;
import com.wjybxx.fastjgame.misc.PlayerMessageFunctionRegistry;
import com.wjybxx.fastjgame.net.RoleType;
import com.wjybxx.fastjgame.net.Session;

import javax.annotation.Nonnull;

/**
 *
 * @author houlei
 * @version 1.0
 * date - 2019/8/26
 */
public class SceneProtocolDispatcherMrg extends ProtocolDispatcherMrg implements PlayerMessageFunctionRegistry, PlayerMessageDispatcher {

	private final PlayerSessionMrg playerSessionMrg;
	private final DefaultPlayerMessageDispatcher messageDispatcher = new DefaultPlayerMessageDispatcher();

	@Inject
	public SceneProtocolDispatcherMrg(PlayerSessionMrg playerSessionMrg) {
		this.playerSessionMrg = playerSessionMrg;
	}

	@Override
	protected final void dispatchOneWayMessage0(Session session, @Nonnull Object message) throws Exception {
		if (session.remoteRole() == RoleType.PLAYER) {
			Player player = playerSessionMrg.getPlayer(session.remoteGuid());
			if (player != null) {
				// 玩家已成功连入场景
				dispatch(player, (AbstractMessage) message);
			} else {
				// TODO 玩家登录
			}
		}
	}

	@Override
	public <T extends AbstractMessage> void register(@Nonnull Class<T> clazz, @Nonnull PlayerMessageFunction<T> handler) {
		messageDispatcher.register(clazz, handler);
	}

	@Override
	public <T extends AbstractMessage> void dispatch(@Nonnull Player player, @Nonnull T message) {
		messageDispatcher.dispatch(player, message);
	}
}
