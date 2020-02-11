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

import com.wjybxx.fastjgame.concurrent.DefaultThreadFactory;
import com.wjybxx.fastjgame.concurrent.EventLoop;
import com.wjybxx.fastjgame.concurrent.ImmediateEventLoop;
import com.wjybxx.fastjgame.concurrent.RejectedExecutionHandlers;
import com.wjybxx.fastjgame.core.LogConsumer;
import com.wjybxx.fastjgame.core.LogPuller;
import com.wjybxx.fastjgame.core.LogPullerFactory;
import com.wjybxx.fastjgame.imp.DefaultGameLogParser;
import com.wjybxx.fastjgame.kafka.KafkaLogPuller;
import com.wjybxx.fastjgame.misc.log.GameLogConsumer;
import com.wjybxx.fastjgame.misc.log.GameLogRecord;
import com.wjybxx.fastjgame.misc.log.LogTopic;
import com.wjybxx.fastjgame.utils.ConcurrentUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 游戏日志消费测试
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/11
 * github - https://github.com/hl845740757
 */
public class KafkaGameLogPullerTest {

    public static void main(String[] args) {
        final LogPullerFactory<GameLogRecord> pullerFactory = KafkaGameLogPullerTest::newPuller;
        final LogPuller puller = pullerFactory.newPuller(Collections.singleton(new TestConsumer()));
        try {
            weakUp(puller);

            waitTerminate(puller);
        } finally {
            puller.shutdown();
        }
    }

    private static KafkaLogPuller<GameLogRecord> newPuller(Collection<LogConsumer<GameLogRecord>> logConsumers) {
        return new KafkaLogPuller<>(
                new DefaultThreadFactory("PULLER"),
                RejectedExecutionHandlers.abort(),
                "localhost:9092",
                "GAME-TEST",
                new DefaultGameLogParser(),
                logConsumers);
    }

    private static void weakUp(LogPuller puller) {
        puller.execute(ConcurrentUtils.NO_OP_TASK);
    }

    private static void waitTerminate(LogPuller puller) {
        puller.terminationFuture().awaitUninterruptibly(5, TimeUnit.MINUTES);
    }

    private static class TestConsumer extends GameLogConsumer {
        @Override
        public EventLoop appEventLoop() {
            return ImmediateEventLoop.INSTANCE;
        }

        @Override
        protected Set<LogTopic> subscribedTopics0() {
            return Collections.singleton(LogTopic.TEST);
        }

        @Override
        public void consume(GameLogRecord record) {
            System.out.println("Thread: " + Thread.currentThread().getName() + ", record: " + record);
        }
    }
}
