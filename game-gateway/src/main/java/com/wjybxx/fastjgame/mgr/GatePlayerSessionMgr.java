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

package com.wjybxx.fastjgame.mgr;

import com.google.inject.Inject;
import com.wjybxx.fastjgame.rpcservice.IGatePlayerSessionMgr;

import java.util.List;

/**
 * 网关服玩家session管理器
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/11/13
 * github - https://github.com/hl845740757
 */
public class GatePlayerSessionMgr implements IGatePlayerSessionMgr {

    @Inject
    public GatePlayerSessionMgr() {
    }

    @Override
    public void sendToPlayer(long playerGuid, byte[] msg) {

    }

    @Override
    public void broadcast(byte[] msg) {

    }

    @Override
    public void broadcast(List<Long> playerGuids, byte[] msg) {

    }
}
