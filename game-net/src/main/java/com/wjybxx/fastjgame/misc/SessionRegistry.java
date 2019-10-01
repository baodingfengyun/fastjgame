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
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import javax.annotation.Nullable;

/**
 * session注册表
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/9/27
 * github - https://github.com/hl845740757
 */
public class SessionRegistry {

    private final Long2ObjectMap<Long2ObjectMap<Session>> guid_guid_session_map = new Long2ObjectOpenHashMap<>();

    public void tick() {
        for (Long2ObjectMap<Session> sessionMap : guid_guid_session_map.values()) {
            for (Session session : sessionMap.values()) {
                // TODO tick内部大量的线程判断在这里是不必要的
                session.tick();
            }
        }
    }

    /**
     * 注册一个session
     *
     * @param session 待注册的session
     */
    public void registerSession(Session session) {
        if (getSession(session.localGuid(), session.remoteGuid()) != null) {
            throw new IllegalArgumentException("session " + session.localGuid() + " - " + session.remoteGuid() + " already registered");
        }
        final Long2ObjectMap<Session> sessionMap = guid_guid_session_map.computeIfAbsent(session.localGuid(),
                k -> new Long2ObjectOpenHashMap<>());
        sessionMap.put(session.remoteGuid(), session);
    }

    /**
     * 删除一个session 。
     * 注意：参数顺序不一样的意义不一样。
     *
     * @param localGuid  自身guid
     * @param remoteGuid 会话另一方guid
     * @return 删除的session
     */
    @Nullable
    public Session removeSession(long localGuid, long remoteGuid) {
        final Long2ObjectMap<Session> sessionMap = guid_guid_session_map.get(localGuid);
        if (null == sessionMap) {
            return null;
        }
        return sessionMap.remove(remoteGuid);
    }

    /**
     * 获取一个session。
     * 注意：参数顺序不一样的意义不一样。
     *
     * @param localGuid  自身guid
     * @param remoteGuid 会话另一方guid
     * @return session
     */
    @Nullable
    public Session getSession(long localGuid, long remoteGuid) {
        final Long2ObjectMap<Session> sessionMap = guid_guid_session_map.get(localGuid);
        if (null == sessionMap) {
            return null;
        }
        return sessionMap.get(remoteGuid);
    }
}
