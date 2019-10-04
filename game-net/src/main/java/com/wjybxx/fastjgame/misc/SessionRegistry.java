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
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Collectors;

/**
 * session注册表
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/9/27
 * github - https://github.com/hl845740757
 */
public class SessionRegistry {

    private final Long2ObjectMap<Session> sessionMap = new Long2ObjectOpenHashMap<>();

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
        if (sessionMap.containsKey(session.sessionGuid())) {
            throw new IllegalArgumentException("session " + session.sessionGuid() + " already registered");
        }
        sessionMap.put(session.sessionGuid(), session);
    }

    /**
     * 删除一个session 。
     *
     * @param sessionGuid session唯一标识
     * @return 删除的session
     */
    @Nullable
    public Session removeSession(long sessionGuid) {
        return sessionMap.remove(sessionGuid);
    }

    /**
     * 获取一个session。
     *
     * @param sessionGuid session唯一标识
     * @return session
     */
    @Nullable
    public Session getSession(long sessionGuid) {
        return sessionMap.get(sessionGuid);
    }

    public void onUserEventLoopTerminal(EventLoop userEventLoop) {
        // 防止迭代的时候删除
        List<Session> needRemovedSession = sessionMap.values().stream()
                .filter(session -> session.localEventLoop() == userEventLoop)
                .collect(Collectors.toList());

        for (Session session : needRemovedSession) {
            session.close();
        }
    }

    public void closeAll() {

    }
}
