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
package com.wjybxx.fastjgame.net.task;

import com.wjybxx.fastjgame.net.RpcCallback;
import com.wjybxx.fastjgame.net.RpcResponse;
import com.wjybxx.fastjgame.net.Session;

/**
 * rpc响应提交任务
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/13
 * github - https://github.com/hl845740757
 */
public class RpcResponseCommitTask implements CommitTask {

    private final Session session;
    private final RpcCallback rpcCallback;
    private final RpcResponse rpcResponse;

    public RpcResponseCommitTask(Session session, RpcCallback rpcCallback, RpcResponse rpcResponse) {
        this.session = session;
        this.rpcResponse = rpcResponse;
        this.rpcCallback = rpcCallback;
    }

    @Override
    public void run() {
        session.config().dispatcher().postRpcCallback(session, rpcCallback, rpcResponse);
    }
}
