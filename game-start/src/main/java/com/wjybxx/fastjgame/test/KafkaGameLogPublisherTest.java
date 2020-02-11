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

package com.wjybxx.fastjgame.test;

import com.wjybxx.fastjgame.concurrent.DefaultThreadFactory;
import com.wjybxx.fastjgame.concurrent.RejectedExecutionHandlers;
import com.wjybxx.fastjgame.core.LogPublisher;
import com.wjybxx.fastjgame.core.LogPublisherFactory;
import com.wjybxx.fastjgame.imp.DefaultGameLogDirector;
import com.wjybxx.fastjgame.kafka.KafkaLogPublisher;
import com.wjybxx.fastjgame.misc.log.GameLogBuilder;
import com.wjybxx.fastjgame.misc.log.LogKey;
import com.wjybxx.fastjgame.misc.log.LogType;
import com.wjybxx.fastjgame.utils.TimeUtils;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * 游戏日志发布器测试
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/11
 * github - https://github.com/hl845740757
 */
public class KafkaGameLogPublisherTest {

    public static void main(String[] args) {
        final LogPublisherFactory<GameLogBuilder> publisherFactory = KafkaGameLogPublisherTest::newPublisher;
        final LogPublisher<GameLogBuilder> publisher = publisherFactory.newPublisher();
        try {
            doProduce(publisher);

            waitTerminate(publisher);
        } finally {
            publisher.shutdown();
        }
    }

    private static LogPublisher<GameLogBuilder> newPublisher() {
        return new KafkaLogPublisher<>(
                new DefaultThreadFactory("PUBLISHER"),
                RejectedExecutionHandlers.abort(),
                "localhost:9092",
                new DefaultGameLogDirector());
    }

    private static void doProduce(LogPublisher<GameLogBuilder> publisher) {
        final long endTime = System.currentTimeMillis() + TimeUtils.MIN * 5;
        for (int playerGuid = 1; System.currentTimeMillis() < endTime; playerGuid++) {
            publisher.publish(newGameLog(playerGuid));
            LockSupport.parkNanos(TimeUtils.NANO_PER_MILLISECOND);
        }
    }

    private static GameLogBuilder newGameLog(long playerGuid) {
        return new GameLogBuilder(LogType.TEST)
                .append(LogKey.playerGuid, playerGuid)
                .append(LogKey.playerName, "wjybxx")
                .append(LogKey.chatContent, "\r\n\t\f\\这是一句没什么用的胡话&=，\r\n\t\f\\只不过带了点特殊字符=&");
    }

    private static void waitTerminate(LogPublisher publisher) {
        publisher.terminationFuture().awaitUninterruptibly(10, TimeUnit.SECONDS);
    }

}
