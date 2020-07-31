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

import com.wjybxx.fastjgame.kafka.log.KafkaLogPublisher;
import com.wjybxx.fastjgame.log.core.LogPublisher;
import com.wjybxx.fastjgame.utils.time.TimeUtils;
import com.wjybxx.fastjgame.utils.concurrent.DefaultThreadFactory;
import com.wjybxx.fastjgame.utils.concurrent.RejectedExecutionHandlers;

import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * kafka日志生产者 - 需要启动kafka
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/11/28
 * github - https://github.com/hl845740757
 */
class KafkaLogPublisherTest {

    public static void main(String[] args) {
        final LogPublisher<GameLogTest> publisher = newProducerEventLoop();
        try {
            doProduce(publisher);

            waitTerminate(publisher);
        } finally {
            publisher.shutdown();
        }
    }

    private static void doProduce(LogPublisher<GameLogTest> publisher) {
        final long endTime = System.currentTimeMillis() + TimeUtils.MIN * 5;
        for (int playerGuid = 1; System.currentTimeMillis() < endTime; playerGuid++) {
            publisher.publish(newLog(playerGuid));
            LockSupport.parkNanos(TimeUtils.NANO_PER_MILLISECOND);
        }
    }

    private static void waitTerminate(LogPublisher publisher) {
        publisher.terminationFuture().awaitUninterruptibly(10, TimeUnit.SECONDS);
    }

    @Nonnull
    private static LogPublisher<GameLogTest> newProducerEventLoop() {
        return new KafkaLogPublisher<>(
                new DefaultThreadFactory("PUBLISHER"),
                RejectedExecutionHandlers.abort(),
                "localhost:9092",
                new LogEncoderTest());
    }

    private static GameLogTest newLog(long playerGuid) {
        return new GameLogTest("TEST")
                .append("playerGuid", playerGuid)
                .append("playerName", "wjybxx")
                .append("chatContent", "\r\n\t\f\\这是一句没什么用的胡话&=，\r\n\t\f\\只不过带了点特殊字符=&");
    }

}
