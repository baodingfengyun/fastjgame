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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Session注册表
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/10/15
 * github - https://github.com/hl845740757
 */
public interface SessionRegistry {

    /**
     * 注册一个session
     *
     * @param session 待注册的session
     */
    void registerSession(@Nonnull AbstractSession session);

    /**
     * @param sessionId session唯一标识
     * @return 移除的session，如果不存在，则返回null
     */
    @Nullable
    AbstractSession removeSession(@Nonnull String sessionId);

    /**
     * 获取一个session。
     *
     * @param sessionId session唯一标识
     * @return session
     */
    @Nullable
    AbstractSession getSession(@Nonnull String sessionId);

    /**
     * 当监听到用户线程关闭时，关闭用户的所有session，并且不发送通知
     *
     * @param appEventLoop 进入终止状态的用户线程
     */
    void onAppEventLoopTerminal(EventLoop appEventLoop);

    /**
     * 关闭注册表中的所有session，并且不发送通知
     */
    void closeAll();
}
