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

package com.wjybxx.fastjgame.test;

import com.google.common.collect.Sets;
import com.wjybxx.fastjgame.concurrent.DefaultThreadFactory;
import com.wjybxx.fastjgame.concurrent.RejectedExecutionHandlers;
import com.wjybxx.fastjgame.misc.log.LogConsumerEventLoop;
import com.wjybxx.fastjgame.misc.log.LogTopic;
import com.wjybxx.fastjgame.utils.ConcurrentUtils;

import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;

/**
 * 日志消费者事件循环
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/11/28
 * github - https://github.com/hl845740757
 */
public class LogConsumerEventLoopTest {

    public static void main(String[] args) {
        final LogConsumerEventLoop consumer = newConsumerEventLoop();

        weakUp(consumer);

        waitTerminate(consumer);
    }

    private static void waitTerminate(LogConsumerEventLoop consumer) {
        consumer.terminationFuture().awaitUninterruptibly(5, TimeUnit.MINUTES);
        consumer.shutdown();
    }

    private static void weakUp(LogConsumerEventLoop consumer) {
        consumer.execute(ConcurrentUtils.NO_OP_TASK);
    }

    @Nonnull
    private static LogConsumerEventLoop newConsumerEventLoop() {
        return new LogConsumerEventLoop("localhost:9092", Sets.newHashSet(LogTopic.TEST.name()), "TEST",
                new DefaultThreadFactory("LOGGER"), RejectedExecutionHandlers.abort());
    }
}
