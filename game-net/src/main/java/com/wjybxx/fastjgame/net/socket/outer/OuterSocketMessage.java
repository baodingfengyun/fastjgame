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

/**
 * 对外的socket消息对象 - 启用了消息确认机制
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/10/1
 * github - https://github.com/hl845740757
 */
public class OuterSocketMessage implements SocketMessage {

    /**
     * 当前包id - 一旦分配就不会改变
     */
    private final long sequence;
    /**
     * 被包装的消息
     */
    private final NetMessage wrappedMessage;
    /**
     * 是否应该调用flush
     */
    private boolean autoFlush;
    /**
     * 消息确认超时时间
     * 每次发送的时候设置超时时间 - 线程封闭(NetEventLoop线程访问)
     */
    private long timeout;

    OuterSocketMessage(long sequence, NetMessage wrappedMessage) {
        this.sequence = sequence;
        this.wrappedMessage = wrappedMessage;
    }

    @Override
    public long getSequence() {
        return sequence;
    }

    @Override
    public NetMessage getWrappedMessage() {
        return wrappedMessage;
    }

    public boolean isAutoFlush() {
        return autoFlush;
    }

    public void setAutoFlush(boolean autoFlush) {
        this.autoFlush = autoFlush;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

}
