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

import com.wjybxx.fastjgame.concurrent.EventLoop;
import com.wjybxx.fastjgame.net.session.Session;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * session注册表
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/9/27
 * github - https://github.com/hl845740757
 */
public class SessionRegistry {

    private final Map<String, Session> sessionMap = new HashMap<>(256);


    public void tick() {
        for (Session session : sessionMap.values()) {
            // TODO tick内部大量的线程判断在这里是不必要的
            session.tick();
        }
    }

    /**
     * 注册一个session
     *
     * @param session 待注册的session
     */
    public void registerSession(Session session) {
        if (sessionMap.containsKey(session.sessionId())) {
            throw new IllegalArgumentException("session " + session.sessionId() + " already registered");
        }
        sessionMap.put(session.sessionId(), session);
    }

    /**
     * 删除一个session 。
     *
     * @param sessionId session唯一标识
     * @return 删除的session
     */
    @Nullable
    public Session removeSession(String sessionId) {
        return sessionMap.remove(sessionId);
    }

    /**
     * 获取一个session。
     * 注意：参数顺序不一样的意义不一样。
     *
     * @param sessionId session唯一标识
     * @return session
     */
    @Nullable
    public Session getSession(String sessionId) {
        return sessionMap.get(sessionId);
    }

    public void onUserEventLoopTerminal(EventLoop userEventLoop) {

    }

    public void closeAll() {

    }
}
