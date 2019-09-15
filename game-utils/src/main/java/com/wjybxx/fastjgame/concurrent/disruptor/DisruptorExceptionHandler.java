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

package com.wjybxx.fastjgame.concurrent.disruptor;

import com.lmax.disruptor.ExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Disruptor需要指定异常处理器，否则默认的异常处理器会导致消费者退出！！！！
 * 要么在这里处理，要么在{@link com.lmax.disruptor.EventHandler#onEvent(Object, long, boolean)}的捕获。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/31
 * github - https://github.com/hl845740757
 */
class DisruptorExceptionHandler implements ExceptionHandler<RunnableEvent> {

    private static final Logger logger = LoggerFactory.getLogger(DisruptorExceptionHandler.class);

    DisruptorExceptionHandler() {
    }

    @Override
    public void handleEventException(Throwable ex, long sequence, RunnableEvent event) {
        // 这里是没有捕获到的异常
        logger.error("onEvent caught exception!", ex);
    }

    @Override
    public void handleOnStartException(Throwable ex) {
        // 启动时抛出异常，则退出线程
        throw new RuntimeException(ex);
    }

    @Override
    public void handleOnShutdownException(Throwable ex) {
        logger.error("onShutdown caught exception!", ex);
    }
}
