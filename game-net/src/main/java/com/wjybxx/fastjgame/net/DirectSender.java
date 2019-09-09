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

import javax.annotation.concurrent.ThreadSafe;

/**
 * 直接发送的sender，在消息数量不是很多的情况下，拥有更低的延迟，吞吐量也较好。
 * 但是如果消息数量非常大，那么延迟会很高(高度竞争)，吞吐量也较差。
 * <p>
 * 该实现没有任何缓存，逻辑简单，是线程安全的，消息顺序也很容易保证。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/30
 * github - https://github.com/hl845740757
 */
@ThreadSafe
public class DirectSender extends AbstractSender {

    public DirectSender(AbstractSession session) {
        super(session);
    }

    @Override
    protected void addSenderTask(SenderTask task) {
        // 直接提交到网络层 - 既有时序保证，又是线程安全的
        netEventLoop().execute(task);
    }

    @Override
    public void flush() {
        // 没有缓存，因此什么都不做
    }

    @Override
    public void clearBuffer() {
        // do nothing
    }

}


