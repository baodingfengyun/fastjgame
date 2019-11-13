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
    private final Session session;
    /**
     * 玩家当前的状态
     */
    private State state = State.LOGIN_GATE;

    public GatePlayerSession(Session session) {
        this.session = session;
    }

    public Session getSession() {
        return session;
    }

    public State getState() {
        return state;
    }

    public enum State {
        /**
         * 登录到网关服 - 初始状态
         */
        LOGIN_GATE,
        /**
         * 登录到中心服 - 等待进入游戏(场景)
         */
        LOGIN_CENTER,
        /**
         * 登录到场景服 - 已进入场景
         */
        LOGIN_SCENE,
        /**
         * 已断开连接
         */
        DISCONNECT,
    }
}
