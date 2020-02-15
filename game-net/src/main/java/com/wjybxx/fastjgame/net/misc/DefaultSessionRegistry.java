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

package com.wjybxx.fastjgame.net.misc;

import com.wjybxx.fastjgame.net.session.AbstractSession;
import com.wjybxx.fastjgame.utils.CollectionUtils;
import com.wjybxx.fastjgame.utils.FunctionUtils;
import com.wjybxx.fastjgame.utils.concurrent.EventLoop;

import javax.annotation.Nonnull;
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
public class DefaultSessionRegistry implements SessionRegistry {

    /**
     * sessionId到session的映射
     */
    private final Map<String, AbstractSession> sessionMap = new HashMap<>(1024);

    @Override
    public void registerSession(@Nonnull AbstractSession session) {
        if (sessionMap.containsKey(session.sessionId())) {
            throw new IllegalArgumentException("session " + session.sessionId() + " already registered");
        }
        sessionMap.put(session.sessionId(), session);
    }

    @Nullable
    @Override
    public AbstractSession removeSession(@Nonnull String sessionId) {
        return sessionMap.remove(sessionId);
    }

    @Nullable
    @Override
    public AbstractSession getSession(@Nonnull String sessionId) {
        return sessionMap.get(sessionId);
    }

    @Override
    public void onAppEventLoopTerminal(EventLoop appEventLoop) {
        // 不要迭代的时候关闭 - 可以删除之后关闭
        CollectionUtils.removeIfAndThen(sessionMap.values(),
                abstractSession -> abstractSession.appEventLoop() == appEventLoop,
                AbstractSession::closeForcibly);
    }

    @Override
    public void closeAll() {
        CollectionUtils.removeIfAndThen(sessionMap.values(),
                FunctionUtils::TRUE,
                AbstractSession::closeForcibly);
    }
}
