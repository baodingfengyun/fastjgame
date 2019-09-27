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

import com.wjybxx.fastjgame.net.Session;

import java.util.List;

/**
 * 批量的单向消息任务
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/9/27
 * github - https://github.com/hl845740757
 */
public class BatchOneWayWriteTask implements WriteTask {

    private final Session session;
    private final List<Object> messageList;

    public BatchOneWayWriteTask(Session session, List<Object> messageList) {
        this.session = session;
        this.messageList = messageList;
    }

    public List<Object> getMessageList() {
        return messageList;
    }

    @Override
    public void run() {
        session.fireWrite(this);
    }

}
