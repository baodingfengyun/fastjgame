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

import com.wjybxx.fastjgame.net.common.NetMessage;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * 有序的消息体，通过ack-sequence实现断线重连/消息确认机制
 * Q: ack为什么不在这？
 * A: ack是每次发送的时候获取最新的ack，在这里{@link SocketMessageTO}。
 * <p>
 * 通过包装的方式，某些方面更加清晰，但有些方面会让人觉得绕的厉害，也增加了一部分内存消耗吧。
 * (本想的内网服务器之间不开启消息确认机制，也不传输sequence和ack，这样可以减少很多不必要的传输，但是那样包装就有点过度了，也导致了维护难度的增加)
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/4/27 11:42
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public interface SocketMessage {

    long getSequence();

    NetMessage getWrappedMessage();
}
