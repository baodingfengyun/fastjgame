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

package com.wjybxx.fastjgame.manager;

import com.google.inject.Inject;
import com.wjybxx.fastjgame.concurrent.EventLoop;
import com.wjybxx.fastjgame.eventloop.NetContextImp;
import com.wjybxx.fastjgame.net.NetContext;
import com.wjybxx.fastjgame.net.RoleType;
import com.wjybxx.fastjgame.utils.CollectionUtils;
import com.wjybxx.fastjgame.utils.FunctionUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.HashSet;
import java.util.Set;

/**
 * 管理分配的{@link com.wjybxx.fastjgame.net.NetContext}。
 * 它通过线程封闭实现线程安全。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/9/29
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public class NetContextManager {

    private static final Logger logger = LoggerFactory.getLogger(NetContextManager.class);

    private final NetEventLoopManager netEventLoopManager;
    private final HttpSessionManager httpSessionManager;
    private final SessionManager sessionManager;
    private NetManagerWrapper managerWrapper;

    /**
     * 已注册的用户的EventLoop集合，它是一个安全措施，如果用户在退出时如果没有执行取消操作，
     * 那么当监听到所在的EventLoop进入终止状态时，取消该EventLoop上注册的用户。
     */
    private final Set<EventLoop> registeredUserEventLoopSet = new HashSet<>();
    /**
     * 已注册的用户集合
     */
    private final Long2ObjectMap<NetContextImp> registeredUserMap = new Long2ObjectOpenHashMap<>();

    @Inject
    public NetContextManager(NetEventLoopManager netEventLoopManager, HttpSessionManager httpSessionManager, SessionManager sessionManager) {
        this.netEventLoopManager = netEventLoopManager;
        this.httpSessionManager = httpSessionManager;
        this.sessionManager = sessionManager;
    }

    public void setManagerWrapper(NetManagerWrapper managerWrapper) {
        this.managerWrapper = managerWrapper;
    }

    public NetContext createContext(long localGuid, RoleType localRole, @Nonnull EventLoop localEventLoop) {
        if (registeredUserMap.containsKey(localGuid)) {
            throw new IllegalArgumentException("user " + localRole + " : " + localGuid + " is already registered!");
        }
        // 创建context
        NetContextImp netContext = new NetContextImp(localGuid, localRole, localEventLoop, netEventLoopManager.eventLoop(), managerWrapper);
        registeredUserMap.put(localGuid, netContext);
        monitor(localEventLoop);
        logger.info("User {}-{} create NetContext!", localRole, localGuid);
        return netContext;
    }

    /**
     * 监听用户线程关闭
     *
     * @param userEventLoop 用户线程
     */
    public void monitor(@Nonnull EventLoop userEventLoop) {
        // 监听用户线程关闭 - 回调到当前线程
        if (registeredUserEventLoopSet.add(userEventLoop)) {
            userEventLoop.terminationFuture().addListener(future -> onUserEventLoopTerminal(userEventLoop),
                    netEventLoopManager.eventLoop());
        }
    }

    public void deregister(NetContext netContext) {
        // 逻辑层调用
        if (registeredUserMap.remove(netContext.localGuid()) == null) {
            return;
        }
        clean(netContext);
    }

    public void clean() {
        CollectionUtils.removeIfAndThen(registeredUserMap.values(),
                FunctionUtils::TRUE,
                this::clean);
    }

    private void clean(NetContext netContext) {
        httpSessionManager.closeUserSession(netContext.localGuid());
        sessionManager.closeUserSession(netContext.localGuid());
        logger.info("User {}-{} NetContext removed!", netContext.localRole(), netContext.localGuid());
    }

    private void onUserEventLoopTerminal(EventLoop userEventLoop) {
        // 删除该EventLoop相关的所有context
        CollectionUtils.removeIfAndThen(registeredUserMap.values(),
                netContext -> netContext.localEventLoop() == userEventLoop,
                this::clean);

        // 更彻底的清理
        managerWrapper.getSessionManager().onUserEventLoopTerminal(userEventLoop);
        managerWrapper.getHttpSessionManager().onUserEventLoopTerminal(userEventLoop);
    }
}
