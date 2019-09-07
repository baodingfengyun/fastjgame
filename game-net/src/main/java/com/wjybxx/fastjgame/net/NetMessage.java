/*
 * Copyright 2019 wjybxx
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wjybxx.fastjgame.net;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * 网络消息包。
 * 它并非线程安全的，但是有多线程下的时序保证，网络层可以安全的访问{@link #timeout}之外的任意字段。
 *
 *  一个包的{@link #sequence}一旦赋值便不会改变，{@link #setSequence(long)} happens-before {@link #getSequence()}。
 *
 * 消息包发送流程：
 * step1  ->  create
 * step2  ->  setSequence,setTimeout
 * step3  ->  write
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/4/27 11:42
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public abstract class NetMessage {
    /**
     * 当前包id。一个网络包一旦被构建，则不再改变！
     */
    private long sequence;
    /**
     * 消息确认超时时间
     * 发送的时候设置超时时间
     */
    private long timeout;

    public NetMessage() {

    }

    public long getSequence() {
        return sequence;
    }

    public void setSequence(long sequence) {
        this.sequence = sequence;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

}
