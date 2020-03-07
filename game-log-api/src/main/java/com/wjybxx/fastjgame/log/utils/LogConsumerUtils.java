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

package com.wjybxx.fastjgame.log.utils;

import com.wjybxx.fastjgame.log.core.LogConsumer;
import com.wjybxx.fastjgame.log.imp.CompositeLogConsumer;
import com.wjybxx.fastjgame.utils.concurrent.EventLoop;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 日志消费者辅助方法类
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/11
 * github - https://github.com/hl845740757
 */
public class LogConsumerUtils {

    private static final Logger logger = LoggerFactory.getLogger(CompositeLogConsumer.class);

    public static <T> void consumeSafely(LogConsumer<T> consumer, T record) {
        try {
            final EventLoop appEventLoop = consumer.appEventLoop();
            if (appEventLoop == null || appEventLoop.inEventLoop()) {
                consumer.consume(record);
            } else {
                appEventLoop.execute(() -> {
                    try {
                        consumer.consume(record);
                    } catch (Throwable e) {
                        ExceptionUtils.rethrow(e);
                    }
                });
            }
        } catch (Throwable e) {
            logger.warn("consumer.consume caught exception", e);
        }
    }
}
