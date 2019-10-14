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

import com.wjybxx.fastjgame.net.common.RpcResponse;
import com.wjybxx.fastjgame.net.session.Session;

/**
 * rpc响应发送任务
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/9/26
 * github - https://github.com/hl845740757
 */
public class RpcResponseWriteTask implements WriteTask {

    private final Session session;
    private final long requestGuid;
    private final boolean sync;
    private final RpcResponse response;

    public RpcResponseWriteTask(Session session, long requestGuid, boolean sync, RpcResponse response) {
        this.session = session;
        this.requestGuid = requestGuid;
        this.sync = sync;
        this.response = response;
    }

    public long getRequestGuid() {
        return requestGuid;
    }

    public boolean isSync() {
        return sync;
    }

    public RpcResponse getResponse() {
        return response;
    }

    @Override
    public void run() {
        if (sync) {
            // 同步调用的结果，需要刷新缓冲区，尽快的返回结果，异步的则无需着急刷新缓冲区
            session.fireWriteAndFlush(this);
        } else {
            session.fireWrite(this);
        }

    }

}
