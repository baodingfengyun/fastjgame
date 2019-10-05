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

package com.wjybxx.fastjgame.manager;

import com.google.inject.Inject;
import com.wjybxx.fastjgame.concurrent.EventLoop;
import com.wjybxx.fastjgame.misc.HttpPortContext;
import com.wjybxx.fastjgame.net.http.HttpRequestCommitTask;
import com.wjybxx.fastjgame.net.http.HttpRequestEvent;
import com.wjybxx.fastjgame.net.http.HttpSessionImp;
import com.wjybxx.fastjgame.timer.FixedDelayHandle;
import com.wjybxx.fastjgame.utils.CollectionUtils;
import com.wjybxx.fastjgame.utils.ConcurrentUtils;
import com.wjybxx.fastjgame.utils.NetUtils;
import com.wjybxx.fastjgame.utils.TimeUtils;
import io.netty.channel.Channel;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * HttpSession管理器。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/1
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public class HttpSessionManager {

    private NetManagerWrapper managerWrapper;
    private final NetEventLoopManager netEventLoopManager;
    private final NetConfigManager netConfigManager;
    private final NetTimeManager netTimeManager;
    /**
     * session映射
     */
    private final Map<Channel, SessionWrapper> sessionWrapperMap = new IdentityHashMap<>(128);

    @Inject
    public HttpSessionManager(NetTimerManager netTimerManager, NetEventLoopManager netEventLoopManager, NetConfigManager netConfigManager,
                              NetTimeManager netTimeManager) {
        this.netEventLoopManager = netEventLoopManager;
        this.netConfigManager = netConfigManager;
        this.netTimeManager = netTimeManager;

        netTimerManager.newFixedDelay(this.netConfigManager.httpSessionTimeout() * TimeUtils.SEC, this::checkSessionTimeout);
    }

    /**
     * 解决循环依赖
     */
    public void setManagerWrapper(NetManagerWrapper managerWrapper) {
        this.managerWrapper = managerWrapper;
    }

    /**
     * 当接收到用户所在eventLoop终止时
     *
     * @param eventLoop 用户所在的eventLoop
     */
    public void onUserEventLoopTerminal(EventLoop eventLoop) {
        assert netEventLoopManager.inEventLoop();
        CollectionUtils.removeIfAndThen(sessionWrapperMap,
                (channel, sessionWrapper) -> sessionWrapper.getSession().localEventLoop() == eventLoop,
                this::closeUserSession);
    }

    private <K, V> void closeUserSession(K k, V v) {

    }

    /**
     * 检查session超时
     */
    private void checkSessionTimeout(FixedDelayHandle handle) {
        // 如果用户持有了httpSession的引用，长时间没有完成响应的话，这里关闭可能导致一些错误
        CollectionUtils.removeIfAndThen(sessionWrapperMap,
                (channel, sessionWrapper) -> netTimeManager.getSystemSecTime() >= sessionWrapper.getSessionTimeout(),
                this::afterRemoved);
    }

    /**
     * 当收到http请求时
     *
     * @param requestEventParam 请参数
     */
    public void onRcvHttpRequest(HttpRequestEvent requestEventParam) {
        final Channel channel = requestEventParam.channel();
        final HttpPortContext portExtraInfo = requestEventParam.getPortExtraInfo();
        // 保存session
        SessionWrapper sessionWrapper = sessionWrapperMap.computeIfAbsent(channel,
                k -> new SessionWrapper(new HttpSessionImp(portExtraInfo.localEventLoop(), netEventLoopManager.eventLoop(), this, channel)));

        // 保持一段时间的活性
        sessionWrapper.setSessionTimeout(netConfigManager.httpSessionTimeout() + netTimeManager.getSystemSecTime());

        final HttpSessionImp httpSession = sessionWrapper.session;

        // 处理请求，提交到用户所在的线程，实现线程安全
        ConcurrentUtils.tryCommit(httpSession.localEventLoop(), new HttpRequestCommitTask(httpSession, requestEventParam.getPath(),
                requestEventParam.getParams(), portExtraInfo.getDispatcher()));
    }

    /**
     * 关闭session。
     */
    public void removeSession(Channel channel) {
        SessionWrapper sessionWrapper = sessionWrapperMap.remove(channel);
        if (null == sessionWrapper) {
            return;
        }
        afterRemoved(channel, sessionWrapper);
    }

    private void afterRemoved(Channel channel, SessionWrapper sessionWrapper) {
        NetUtils.closeQuietly(channel);
    }

    public void clean() {

    }

    private static class SessionWrapper {

        private final HttpSessionImp session;
        /**
         * 会话超时时间 - 避免对外，线程安全问题
         */
        private int sessionTimeout;

        private SessionWrapper(HttpSessionImp session) {
            this.session = session;
        }

        HttpSessionImp getSession() {
            return session;
        }

        int getSessionTimeout() {
            return sessionTimeout;
        }

        void setSessionTimeout(int sessionTimeout) {
            this.sessionTimeout = sessionTimeout;
        }
    }
}
