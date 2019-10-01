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
import com.google.protobuf.Message;
import com.google.protobuf.Message.Builder;
import com.wjybxx.fastjgame.gameobject.GameObject;
import com.wjybxx.fastjgame.gameobject.Player;
import com.wjybxx.fastjgame.misc.PlatformType;
import com.wjybxx.fastjgame.misc.ViewGrid;
import com.wjybxx.fastjgame.net.session.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Predicate;

/**
 * 对会话信息的封装，提供良好的接口，方便使用；
 * <p>
 * 注意：广播消息，避免造成错误，必须是构建完成的消息对象。
 * 只对单个玩家发送时，这里提供发送builder对象的接口；
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/6/4 18:22
 * github - https://github.com/hl845740757
 */
public class SceneSendMrg {

    private static final Logger logger = LoggerFactory.getLogger(SceneSendMrg.class);
    /**
     * center服在scene服中的信息
     */
    private final CenterInSceneInfoMrg centerInSceneInfoMrg;
    private final PlayerSessionMrg playerSessionMrg;

    @Inject
    public SceneSendMrg(CenterInSceneInfoMrg centerInSceneInfoMrg, PlayerSessionMrg playerSessionMrg) {
        this.centerInSceneInfoMrg = centerInSceneInfoMrg;
        this.playerSessionMrg = playerSessionMrg;
    }

    /**
     * 发送消息给玩家
     *
     * @param player 玩家
     * @param msg    消息
     */
    public void sendToPlayer(Player player, Message msg) {
        if (null != player.getSession()) {
            player.getSession().send(msg);
        }
    }

    /**
     * 发送消息给玩家
     *
     * @param player  玩家
     * @param builder 还未真正构造的消息对象
     */
    public void sendToPlayer(Player player, Builder builder) {
        sendToPlayer(player, builder.build());
    }

    /**
     * 发送到玩家所在的center服
     *
     * @param player 玩家
     * @param msg    消息
     */
    public void sendToCenter(Player player, Message msg) {
        sendToCenter(player.getPlatformType(), player.getActualServerId(), msg);
    }

    /**
     * 发送到指定中心服
     *
     * @param platformType 中心服所在的平台
     * @param serverId     中心服的id
     * @param msg          消息
     */
    public void sendToCenter(PlatformType platformType, int serverId, Message msg) {
        Session session = centerInSceneInfoMrg.getCenterSession(platformType, serverId);
        if (session == null) {
            logger.warn("send to disconnected center {}-{}", platformType, serverId);
            return;
        }
        session.send(msg);
    }

    /**
     * 广播指定对象视野内的所有玩家，如果gameobject为玩家，包括自己
     *
     * @param gameObject 广播中心对象
     * @param msg        广播消息
     */
    public void broadcastPlayer(GameObject gameObject, Message msg) {
        ViewGrid centerViewGrid = gameObject.getViewGrid();
        broadcastPlayer(centerViewGrid.getViewableGrids(), msg);
    }

    /**
     * 广播指定视野格子的玩家
     *
     * @param viewGrids 视野格子
     * @param msg       消息
     */
    public void broadcastPlayer(List<ViewGrid> viewGrids, Message msg) {
        for (ViewGrid viewGrid : viewGrids) {
            if (viewGrid.getPlayerNum() <= 0) {
                continue;
            }
            for (Player player : viewGrid.getPlayerSet()) {
                sendToPlayer(player, msg);
            }
        }
    }

    /**
     * 广播指定对象视野内的所有玩家，去除掉指定玩家
     *
     * @param gameObject   广播中心对象
     * @param msg          消息对象
     * @param exceptPlayer 不广播该玩家
     */
    public void broadcastPlayerExcept(GameObject gameObject, Message msg, Player exceptPlayer) {
        ViewGrid centerViewGrid = gameObject.getViewGrid();
        broadcastPlayerExcept(centerViewGrid.getViewableGrids(), msg, exceptPlayer);
    }

    /**
     * 广播指定视野格子内的玩家，去除指定玩家
     *
     * @param viewGrids    视野格子
     * @param msg          消息
     * @param exceptPlayer 去除的玩家
     */
    public void broadcastPlayerExcept(List<ViewGrid> viewGrids, Message msg, Player exceptPlayer) {
        for (ViewGrid viewGrid : viewGrids) {
            if (viewGrid.getPlayerNum() <= 0) {
                continue;
            }
            for (Player player : viewGrid.getPlayerSet()) {
                // 去除指定玩家
                if (player == exceptPlayer) {
                    continue;
                }
                sendToPlayer(player, msg);
            }
        }
    }

    /**
     * 广播指定对象视野内的所有玩家，去除掉指定条件的玩家
     *
     * @param gameObject 广播中心对象
     * @param msg        广播消息
     * @param except     排除条件，true的不广播
     *                   {@link com.wjybxx.fastjgame.misc.SceneBroadcastFilters}可能会有帮助
     */
    public void broadcastPlayerExcept(GameObject gameObject, Message msg, Predicate<Player> except) {
        ViewGrid centerViewGrid = gameObject.getViewGrid();
        broadcastPlayerExcept(centerViewGrid.getViewableGrids(), msg, except);
    }

    /**
     * 广播指定视野格子的玩家，去除掉指定条件的玩家
     *
     * @param viewGrids 指定的视野格子
     * @param msg       消息
     * @param except    排除条件，true的不广播
     *                  {@link com.wjybxx.fastjgame.misc.SceneBroadcastFilters}可能会有帮助
     */
    public void broadcastPlayerExcept(List<ViewGrid> viewGrids, Message msg, Predicate<Player> except) {
        for (ViewGrid viewGrid : viewGrids) {
            if (viewGrid.getPlayerNum() <= 0) {
                continue;
            }
            for (Player player : viewGrid.getPlayerSet()) {
                if (!except.test(player)) {
                    sendToPlayer(player, msg);
                }
            }
        }
    }
}
