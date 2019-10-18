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

package com.wjybxx.fastjgame.net.socket.outer;

import com.wjybxx.fastjgame.net.common.NetMessage;
import com.wjybxx.fastjgame.net.socket.SocketMessage;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * 对外的socket消息对象 - 启用了消息确认机制
 * 它并非线程安全的，通过以下方式保证安全性：
 * 1. netty线程只会访问{@link #wrappedMessage}和{@link #sequence}，这俩一旦赋值便不会变更
 * 2. {@link #setSequence(long)} happens - before {@link #getSequence()}
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/10/1
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public class OuterSocketMessage implements SocketMessage {

    /**
     * 被包装的消息
     */
    private final NetMessage wrappedMessage;
    /**
     * 当前包id - 一旦分配就不会改变
     */
    private long sequence;
    /**
     * 消息确认超时时间
     * 每次发送的时候设置超时时间 - 线程封闭(NetEventLoop线程访问)
     */
    private long ackDeadline;
    /**
     * 在发送之后是否使用心跳包进行了跟踪
     */
    private boolean traced = false;

    OuterSocketMessage(NetMessage wrappedMessage) {
        this.wrappedMessage = wrappedMessage;
    }

    public void setSequence(long sequence) {
        this.sequence = sequence;
    }

    @Override
    public long getSequence() {
        return sequence;
    }

    @Override
    public NetMessage getWrappedMessage() {
        return wrappedMessage;
    }

    public long getAckDeadline() {
        return ackDeadline;
    }

    public void setAckDeadline(long ackDeadline) {
        this.ackDeadline = ackDeadline;
    }

    public boolean isTraced() {
        return traced;
    }

    public void setTraced(boolean traced) {
        this.traced = traced;
    }
}
