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

package com.wjybxx.fastjgame.net;

import com.wjybxx.fastjgame.eventloop.NetEventLoop;
import com.wjybxx.fastjgame.manager.NetConfigManager;

/**
 * Session消息发送模式
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/31
 * github - https://github.com/hl845740757
 */
public enum SessionSenderMode {

    /**
     * 直接发送的{@link Sender}，该模式下的{@link Sender}不会对任何消息进行缓存，会立即提交到网络层。因此能提供全局的异步消息顺序保证！！！
     * 该模式在数据包量较少的时候拥有较高的吞吐量以及较低的延迟。在数据包较多的情况下，由于{@link NetEventLoop}竞争较为激烈，使得吞吐量会降低，延迟也会增加。
     * <p>
     * 它有着较低的理解成本，使用简单，线程安全，有更多的保证，如果数据包量不是特别大，建议使用该模式。
     * 用户不需要调用{@link Session#flush()}。
     */
    DIRECT,

    /**
     * 不可共享模式，该模式下，任意发送消息的方法只能用户线程{@link Session#localEventLoop()}使用。
     * 包括发送单向消息、rpc请求、通过{@link RpcResponseChannel}返回rpc结果，都只能用户线程使用（NetContext中注册的用户）。
     * 如果你确定所有的消息发送始终在用户线程，那么使用该模式将获得最好的性能和吞吐量。
     * <p>
     * 该模式在数据量较少的时候，延迟上差于{@link #DIRECT}，且吞吐量上没有明显优势。但是在数据量较多的时候，能够有效降低竞争，
     * 从而拥有更高的吞吐量，且延迟相对稳定。
     * <p>
     * 最大缓存数量由{@link NetConfigManager#flushThreshold()}决定，当到达该值时，会自动刷新缓冲区。
     * 为避免消息残留，用户必须在特定的时候调用{@link Session#flush()}刷新缓冲区（eg：刷帧时）。
     * <p>
     * 如果想了解为什么也不允许{@link RpcResponseChannel}跨线程调用，请查看{@link UnsharableSender}类文档。
     */
    UNSHARABLE,
}
