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

package com.wjybxx.fastjgame.kafka.logtest;

import com.wjybxx.fastjgame.kafka.log.KafkaLogPuller;
import com.wjybxx.fastjgame.log.core.LogConsumer;
import com.wjybxx.fastjgame.log.core.LogPuller;
import com.wjybxx.fastjgame.log.imp.DefaultLogParser;
import com.wjybxx.fastjgame.utils.ConcurrentUtils;
import com.wjybxx.fastjgame.utils.concurrent.DefaultThreadFactory;
import com.wjybxx.fastjgame.utils.concurrent.EventLoop;
import com.wjybxx.fastjgame.utils.concurrent.ImmediateEventLoop;
import com.wjybxx.fastjgame.utils.concurrent.RejectedExecutionHandlers;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * kafka日志消费者 - 需要启动kafka
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/11/28
 * github - https://github.com/hl845740757
 */
public class KafkaLogPullerTest {

    public static void main(String[] args) {
        final LogPuller puller = newKafkaLogPuller();
        try {
            weakUp(puller);

            waitTerminate(puller);
        } finally {
            puller.shutdown();
        }
    }

    private static void waitTerminate(LogPuller puller) {
        puller.terminationFuture().awaitUninterruptibly(5, TimeUnit.MINUTES);
    }

    private static void weakUp(LogPuller puller) {
        puller.execute(ConcurrentUtils.NO_OP_TASK);
    }

    @Nonnull
    private static LogPuller newKafkaLogPuller() {
        return new KafkaLogPuller<>(new DefaultThreadFactory("PULLER"),
                RejectedExecutionHandlers.abort(),
                "localhost:9092",
                "GROUP-TEST",
                new DefaultLogParser(),
                Collections.singleton(new TestLogConsumer<>()));
    }

    private static class TestLogConsumer<T> implements LogConsumer<T> {

        @Override
        public EventLoop appEventLoop() {
            return ImmediateEventLoop.INSTANCE;
        }

        @Override
        public Set<String> subscribedTopics() {
            return Collections.singleton("TEST");
        }

        @Override
        public void consume(T record) throws Exception {
            System.out.println("Thread: " + Thread.currentThread().getName() + ", record: " + record);
        }
    }
}
