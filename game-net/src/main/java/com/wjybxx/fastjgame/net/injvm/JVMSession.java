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

import com.wjybxx.fastjgame.manager.NetManagerWrapper;
import com.wjybxx.fastjgame.net.AbstractSession;
import com.wjybxx.fastjgame.net.NetContext;
import com.wjybxx.fastjgame.net.RoleType;
import com.wjybxx.fastjgame.net.Session;
import com.wjybxx.fastjgame.net.pipeline.SessionConfig;

/**
 * JVM内部会话
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/9/9
 * github - https://github.com/hl845740757
 */
public class JVMSession extends AbstractSession {

    /**
     * 配置信息
     */
    private final JVMSessionConfig sessionConfig;
    /**
     * 会话另一方的信息
     */
    private Session remoteSession;

    public JVMSession(NetContext netContext, NetManagerWrapper managerWrapper, JVMSessionConfig sessionConfig) {
        super(netContext, managerWrapper);
        this.sessionConfig = sessionConfig;
    }

    public Session getRemoteSession() {
        return remoteSession;
    }

    public void setRemoteSession(Session remoteSession) {
        this.remoteSession = remoteSession;
    }

    @Override
    public SessionConfig config() {
        return sessionConfig;
    }

    @Override
    public long remoteGuid() {
        return remoteSession.localGuid();
    }

    @Override
    public RoleType remoteRole() {
        return remoteSession.localRole();
    }

    @Override
    public boolean isActive() {
        return false;
    }
}
