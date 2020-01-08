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

import com.wjybxx.fastjgame.async.GenericFutureResultListener;
import com.wjybxx.fastjgame.net.common.RpcFutureResult;
import com.wjybxx.fastjgame.net.session.Session;

/**
 * rpc响应提交任务
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/13
 * github - https://github.com/hl845740757
 */
public class RpcResponseCommitTask<V> implements CommitTask {

    private final Session session;
    private final RpcFutureResult<V> futureResult;
    private final GenericFutureResultListener<RpcFutureResult<V>, ? super V> listener;

    public RpcResponseCommitTask(Session session, RpcFutureResult<V> futureResult,
                                 GenericFutureResultListener<RpcFutureResult<V>, ? super V> listener) {
        this.session = session;
        this.listener = listener;
        this.futureResult = futureResult;
    }

    @Override
    public void run() {
        session.config().dispatcher().postRpcCallback(session, listener, futureResult);
    }
}
