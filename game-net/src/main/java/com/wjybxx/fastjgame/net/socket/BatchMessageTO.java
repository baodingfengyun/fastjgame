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

import java.util.List;

/**
 * 批量消息传输对象 - 可减少与IO线程的交互
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/9/28
 * github - https://github.com/hl845740757
 */
@TransferObject
public class BatchMessageTO {

    private final List<NetMessage> messageList;

    public BatchMessageTO(List<NetMessage> messageList) {
        this.messageList = messageList;
    }

    public List<NetMessage> getMessageList() {
        return messageList;
    }
}
