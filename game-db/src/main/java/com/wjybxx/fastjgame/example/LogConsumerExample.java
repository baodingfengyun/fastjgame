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

package com.wjybxx.fastjgame.example;

import com.wjybxx.fastjgame.concurrent.DefaultThreadFactory;
import com.wjybxx.fastjgame.concurrent.RejectedExecutionHandlers;
import com.wjybxx.fastjgame.kafka.LogConsumerEventLoop;
import com.wjybxx.fastjgame.utils.ConcurrentUtils;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * kafka日志消费者 - 需要启动kafka
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/11/28
 * github - https://github.com/hl845740757
 */
public class LogConsumerExample {

    public static void main(String[] args) {
        final LogConsumerEventLoop consumer = newConsumerEventLoop();
        try {
            weakUp(consumer);

            waitTerminate(consumer);
        } finally {
            consumer.shutdown();
        }
    }

    private static void waitTerminate(LogConsumerEventLoop consumer) {
        consumer.terminationFuture().awaitUninterruptibly(5, TimeUnit.MINUTES);
    }

    private static void weakUp(LogConsumerEventLoop consumer) {
        consumer.execute(ConcurrentUtils.NO_OP_TASK);
    }

    @Nonnull
    private static LogConsumerEventLoop newConsumerEventLoop() {
        return new LogConsumerEventLoop(new DefaultThreadFactory("CONSUMER"),
                RejectedExecutionHandlers.abort(),
                "localhost:9092", Collections.singleton("TEST"),
                "TEST",
                System.out::println);
    }
}
