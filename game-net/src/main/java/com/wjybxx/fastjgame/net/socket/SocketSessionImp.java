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

package com.wjybxx.fastjgame.net.socket;

import com.wjybxx.fastjgame.eventloop.NetContext;
import com.wjybxx.fastjgame.manager.NetManagerWrapper;
import com.wjybxx.fastjgame.net.session.AbstractSession;

/**
 * 通过socket建立的远程连接。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/9/9
 * github - https://github.com/hl845740757
 */
public class SocketSessionImp extends AbstractSession implements SocketSession {

    private final SocketSessionConfig config;

    public SocketSessionImp(NetContext netContext, String sessionId, long remoteGuid, NetManagerWrapper managerWrapper,
                            SocketSessionConfig config) {
        super(netContext, sessionId, remoteGuid, managerWrapper);
        this.config = config;
    }

    @Override
    public SocketSessionConfig config() {
        return config;
    }
}
