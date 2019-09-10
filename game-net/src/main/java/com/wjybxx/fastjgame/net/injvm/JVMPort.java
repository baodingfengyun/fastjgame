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

package com.wjybxx.fastjgame.net.injvm;

import com.wjybxx.fastjgame.eventloop.NetEventLoop;
import com.wjybxx.fastjgame.manager.JVMC2SSessionManager;
import com.wjybxx.fastjgame.manager.NetManagerWrapper;
import com.wjybxx.fastjgame.net.*;

import javax.annotation.concurrent.ThreadSafe;

/**
 * 用于建立JVM内部session的“端口”，它并非一个真正的端口。
 * 注意：每次调用{@link NetContext#bindInJVM(SessionLifecycleAware, ProtocolDispatcher, SessionSenderMode)}都会产生一个新的jvmPort。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/9/9
 * github - https://github.com/hl845740757
 */
@ThreadSafe
public class JVMPort {

    /**
     * 本地角色guid
     */
    private final long localGuid;
    /**
     * 本地角色类型
     */
    private final RoleType localRole;
    /**
     * 该端口上的session处理逻辑
     */
    private final PortContext portContext;
    /**
     * 获取对应的管理器
     */
    private final NetManagerWrapper managerWrapper;

    public JVMPort(long localGuid, RoleType localRole, PortContext portContext, NetManagerWrapper managerWrapper) {
        this.portContext = portContext;
        this.localGuid = localGuid;
        this.localRole = localRole;
        this.managerWrapper = managerWrapper;
    }

    public PortContext getPortContext() {
        assert netEventLoop().inEventLoop();
        return portContext;
    }

    public long localGuid() {
        return localGuid;
    }

    public RoleType localRole() {
        return localRole;
    }

    /**
     * Q: 为何要使用绑定方的{@link NetEventLoop}？
     * A: 因为连接的请求方所在的线程是不确定的，而接收方所在的线程是确定的。
     */
    public NetEventLoop netEventLoop() {
        return managerWrapper.getNetEventLoopManager().eventLoop();
    }

    public JVMC2SSessionManager getSessionManager() {
        assert netEventLoop().inEventLoop();
        return managerWrapper.getJvmc2SSessionManager();
    }
}
