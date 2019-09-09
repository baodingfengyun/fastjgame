/*
 *    Copyright 2019 wjybxx
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.wjybxx.fastjgame.net;

/**
 * 单向消息任务
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/8
 * github - https://github.com/hl845740757
 */
public class OneWayMessageCommitTask implements CommitTask {

    private Session session;
    /**
     * 消息分发器
     */
    private ProtocolDispatcher protocolDispatcher;
    /**
     * 单向消息的内容
     */
    private Object message;

    public OneWayMessageCommitTask(Session session, ProtocolDispatcher protocolDispatcher, Object message) {
        this.session = session;
        this.protocolDispatcher = protocolDispatcher;
        this.message = message;
    }

    @Override
    public void run() {
        protocolDispatcher.postOneWayMessage(session, message);
    }
}
