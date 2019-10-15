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
import com.wjybxx.fastjgame.net.session.AbstractSession;
import com.wjybxx.fastjgame.utils.CollectionUtils;
import com.wjybxx.fastjgame.utils.FunctionUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Iterator;
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

    /**
     * sessionId到session的映射
     * (发现session只用于服务器与服务器之间，因此不需要太高的容量)
     */
    private final Map<String, AbstractSession> sessionMap = new HashMap<>(32);

    public void tick() {
        final Iterator<AbstractSession> itr = sessionMap.values().iterator();
        while (itr.hasNext()) {
            AbstractSession session = itr.next();
            if (session.isClosed()) {
                // 延迟删除的session
                itr.remove();
            } else {
                session.tick();
                // tick过程中关闭的session
                if (session.isClosed()) {
                    itr.remove();
                }
            }
        }
    }

    /**
     * 注册一个session
     *
     * @param session 待注册的session
     */
    public void registerSession(@Nonnull AbstractSession session) {
        if (sessionMap.containsKey(session.sessionId())) {
            throw new IllegalArgumentException("session " + session.sessionId() + " already registered");
        }
        sessionMap.put(session.sessionId(), session);
    }


    /**
     * 获取一个session。
     *
     * @param sessionId session唯一标识
     * @return session
     */
    @Nullable
    public AbstractSession getSession(@Nonnull String sessionId) {
        return sessionMap.get(sessionId);
    }

    public void onUserEventLoopTerminal(EventLoop userEventLoop) {
        CollectionUtils.removeIfAndThen(sessionMap.values(),
                abstractSession -> abstractSession.localEventLoop() == userEventLoop,
                AbstractSession::closeForcibly);
    }

    public void closeAll() {
        CollectionUtils.removeIfAndThen(sessionMap.values(),
                FunctionUtils::TRUE,
                AbstractSession::closeForcibly);
    }
}
