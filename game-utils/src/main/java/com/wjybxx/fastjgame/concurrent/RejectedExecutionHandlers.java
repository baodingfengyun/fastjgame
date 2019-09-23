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

package com.wjybxx.fastjgame.concurrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.RejectedExecutionException;

/**
 * 常用的拒绝策略。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/19 23:56
 * github - https://github.com/hl845740757
 */
public class RejectedExecutionHandlers {

    private static final Logger logger = LoggerFactory.getLogger(RejectedExecutionHandlers.class);

    /**
     * 中止 - 抛出异常
     */
    private static final RejectedExecutionHandler ABORT_POLICY = (r, eventLoop) -> {
        throw new RejectedExecutionException();
    };

    /**
     * 调用者执行
     */
    private static final RejectedExecutionHandler CALLER_RUNS_POLICY = (r, eventLoop) -> {
        r.run();
    };

    /**
     * 忽略
     */
    private static final RejectedExecutionHandler DISCARD_POLICY = (r, eventLoop) -> {
        // do nothing
    };


    /**
     * 仅仅记录一条日志
     */
    private static final RejectedExecutionHandler LOG_POLICY = (r, eventLoop) -> {
        logger.info("task {} is reject by {}.", r.getClass().getCanonicalName(), eventLoop.getClass().getCanonicalName());
    };


    private RejectedExecutionHandlers() {

    }

    /**
     * 抛出拒绝异常
     */
    public static RejectedExecutionHandler abort() {
        return ABORT_POLICY;
    }

    /**
     * 调用者执行策略：调用execute方法的线程执行。
     * 注意：
     * 1. 需要访问其它线程执行的任务不能使用该策略。
     * 2. 必须有序执行的任务不能使用该策略。
     */
    public static RejectedExecutionHandler callerRuns() {
        return CALLER_RUNS_POLICY;
    }

    /**
     * 丢弃异常/忽略异常
     */
    public static RejectedExecutionHandler discard() {
        return DISCARD_POLICY;
    }

    /**
     * 仅仅是记录一条日志
     */
    public static RejectedExecutionHandler log() {
        return LOG_POLICY;
    }
}
