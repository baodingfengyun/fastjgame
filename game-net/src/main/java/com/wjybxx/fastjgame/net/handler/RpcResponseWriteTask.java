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

package com.wjybxx.fastjgame.net.handler;

import com.wjybxx.fastjgame.net.RpcResponse;
import com.wjybxx.fastjgame.net.Session;

/**
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
        session.fireWrite(this);
    }

}
