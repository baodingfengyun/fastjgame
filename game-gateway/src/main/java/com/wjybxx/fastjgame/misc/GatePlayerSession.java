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

package com.wjybxx.fastjgame.misc;

import com.wjybxx.fastjgame.net.session.Session;

/**
 * 玩家在网关服的session
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/11/13
 * github - https://github.com/hl845740757
 */
public class GatePlayerSession {

    /**
     * 玩家的真实session
     */
    private final Session playerSession;

    /**
     * 需要转发到的scene服务器
     * 中心服是唯一的，因此不需要保存中心服session引用。
     */
    private Session sceneSession;

    /**
     * 玩家当前的状态
     */
    private State state = State.LOGIN_GATE;

    public GatePlayerSession(Session playerSession) {
        this.playerSession = playerSession;
    }

    public long getPlayerGuid() {
        return playerSession.remoteGuid();
    }

    public Session getPlayerSession() {
        return playerSession;
    }

    public Session getSceneSession() {
        return sceneSession;
    }

    public void setSceneSession(Session sceneSession) {
        this.sceneSession = sceneSession;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public enum State {
        /**
         * 已登录到网关服 - 初始状态
         * 它表示玩家还未连接到中心服，可能在排队。
         */
        LOGIN_GATE,
        /**
         * 已登录到中心服 - 等待进入游戏(选角)
         */
        LOGIN_CENTER,
        /**
         * 登录到场景服 - 已选择角色，正在进入场景或已进入场景
         */
        LOGIN_SCENE,
        /**
         * 已断开连接
         */
        DISCONNECT,
    }
}
