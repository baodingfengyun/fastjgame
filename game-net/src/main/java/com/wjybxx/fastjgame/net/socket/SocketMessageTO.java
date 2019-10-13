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

/**
 * 单个{@link SocketMessage}对应的传输对象 - 之所以需要该对象进行传输，是因为ack字段是发送的时候才赋值的
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/9/7
 * github - https://github.com/hl845740757
 */
public interface SocketMessageTO {

    long getAck();

    SocketMessage getSocketMessage();
}
