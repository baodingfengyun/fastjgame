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

import com.wjybxx.fastjgame.concurrent.ListenableFuture;
import com.wjybxx.fastjgame.manager.JVMS2CSessionManager;
import com.wjybxx.fastjgame.manager.NetManagerWrapper;
import com.wjybxx.fastjgame.manager.SessionManager;
import com.wjybxx.fastjgame.net.NetContext;
import com.wjybxx.fastjgame.net.RoleType;
import com.wjybxx.fastjgame.net.SessionSenderMode;
import com.wjybxx.fastjgame.utils.EventLoopUtils;

import javax.annotation.Nonnull;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * session的接收方
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/9/9
 * github - https://github.com/hl845740757
 */
public class JVMS2CSession extends AbstractJVMSession {

    /**
     * 会话存活状态，创建session的时候已是激活的了。
     */
    private final AtomicBoolean state = new AtomicBoolean(true);

    public JVMS2CSession(NetContext netContext, long remoteGuid, RoleType remoteRole,
                         NetManagerWrapper managerWrapper, SessionSenderMode sessionSenderMode) {
        super(netContext, remoteGuid, remoteRole, managerWrapper, sessionSenderMode);
    }

    @Nonnull
    @Override
    protected JVMS2CSessionManager getSessionManager() {
        return managerWrapper.getJvms2CSessionManager();
    }

    @Override
    public boolean isActive() {
        return state.get();
    }

    public void setClosed() {
        state.set(false);
    }

    @Override
    public ListenableFuture<?> close() {
        setClosed();
        return EventLoopUtils.submitOrRun(netEventLoop(), () -> {
            getSessionManager().removeSession(localGuid(), remoteGuid, "close");
        });
    }
}
