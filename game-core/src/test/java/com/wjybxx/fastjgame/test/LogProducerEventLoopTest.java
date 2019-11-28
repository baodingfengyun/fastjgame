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
import com.wjybxx.fastjgame.concurrent.RejectedExecutionHandlers;
import com.wjybxx.fastjgame.misc.log.LogBuilder;
import com.wjybxx.fastjgame.misc.log.LogKey;
import com.wjybxx.fastjgame.misc.log.LogProducerEventLoop;
import com.wjybxx.fastjgame.misc.log.LogType;

import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * 测试发送消息到kafka
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/11/28
 * github - https://github.com/hl845740757
 */
public class LogProducerEventLoopTest {

    public static void main(String[] args) {
        final LogProducerEventLoop producer = newProducerEventLoop();

        doProduce(producer);

        waitTerminate(producer);
    }

    private static void doProduce(LogProducerEventLoop producer) {
        IntStream.rangeClosed(1, 10000).forEach(playerGuid -> {
            producer.log(newLog(playerGuid));
        });
    }

    private static void waitTerminate(LogProducerEventLoop producer) {
        producer.terminationFuture().awaitUninterruptibly(10, TimeUnit.SECONDS);
        producer.shutdown();
    }

    @Nonnull
    private static LogProducerEventLoop newProducerEventLoop() {
        return new LogProducerEventLoop("localhost:9092",
                new DefaultThreadFactory("LOGGER"),
                RejectedExecutionHandlers.abort());
    }

    private static LogBuilder newLog(long playerGuid) {
        return new LogBuilder(LogType.TEST)
                .append(LogKey.playerName, "wjybxx")
                .append(LogKey.playerGuid, playerGuid)
                .append(LogKey.chatContent, "这是一句没什么用的胡话&=，只不过带了点屏蔽字符=&");
    }
}
