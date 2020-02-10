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

package com.wjybxx.fastjgame.mgr;

import com.google.inject.Inject;
import com.wjybxx.fastjgame.concurrent.DefaultThreadFactory;
import com.wjybxx.fastjgame.concurrent.RejectedExecutionHandlers;
import com.wjybxx.fastjgame.kafka.LogProducerEventLoop;
import com.wjybxx.fastjgame.misc.log.GameLogBuilder;
import com.wjybxx.fastjgame.misc.log.GameKafkaLogDirector;
import com.wjybxx.fastjgame.utils.ConcurrentUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

/**
 * 游戏埋点日志线程管理器
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/11/27
 * github - https://github.com/hl845740757
 */
@ThreadSafe
public class LogProducerMgr {

    private static final Logger logger = LoggerFactory.getLogger(LogProducerMgr.class);

    private final LogProducerEventLoop<GameLogBuilder> producer;
    private volatile boolean shutdown = false;

    @Inject
    public LogProducerMgr(GameConfigMgr gameConfigMgr) {
        producer = newProducer(gameConfigMgr);
    }

    @Nonnull
    private static LogProducerEventLoop<GameLogBuilder> newProducer(GameConfigMgr gameConfigMgr) {
        return new LogProducerEventLoop<>(new DefaultThreadFactory("LOG-PRODUCER"),
                RejectedExecutionHandlers.log(),
                gameConfigMgr.getKafkaBrokerList(), new GameKafkaLogDirector());
    }

    /**
     * 启动kafka线程
     */
    public void start() {
        producer.terminationFuture().addListener(future -> {
            if (!shutdown) {
                logger.error("producer shutdown by mistake");
            }
        });

        producer.execute(ConcurrentUtils.NO_OP_TASK);
    }

    /**
     * 关闭kafka线程
     */
    public void shutdown() {
        shutdown = true;
        producer.shutdown();
        logger.info("LogProducer shutdown success");
    }

    /**
     * 发送消息到kafka
     */
    public void publish(GameLogBuilder logBuilder) {
        producer.publish(logBuilder);
    }
}
