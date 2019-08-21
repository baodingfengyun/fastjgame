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

    private static final RejectedExecutionHandler REJECT_POLICY = new RejectedExecutionHandler() {
        @Override
        public void rejected(Runnable r, EventLoop eventLoop) {
            throw new RejectedExecutionException();
        }
    };

    private static final RejectedExecutionHandler CALLER_RUNS_POLICY = new RejectedExecutionHandler() {
        @Override
        public void rejected(Runnable r, EventLoop eventLoop) {
            r.run();
        }
    };

    /** 忽略 */
    private static final RejectedExecutionHandler DISCARD_POLICY = new RejectedExecutionHandler() {
        @Override
        public void rejected(Runnable r, EventLoop eventLoop) {
            // do nothing
        }
    };


    /** 仅仅记录一条日志 */
    private static final RejectedExecutionHandler LOG_POLICY = new RejectedExecutionHandler() {
        @Override
        public void rejected(Runnable r, EventLoop eventLoop) {
            logger.warn("task {} is reject by {}.", r.getClass().getCanonicalName(), eventLoop.getClass().getCanonicalName());
        }
    };


    private RejectedExecutionHandlers() {

    }

    /** 抛出拒绝异常 */
    public static RejectedExecutionHandler reject() {
        return REJECT_POLICY;
    }

    /**
     * 调用者execute的线程执行。
     * 注意：
     * 1. 需要访问其它线程执行的任务不能使用该策略。
     * 2. 必须有序执行的任务不能使用该策略。
     */
    public static RejectedExecutionHandler callerRuns() {
        return CALLER_RUNS_POLICY;
    }

    /** 丢弃异常/忽略异常 */
    public static RejectedExecutionHandler discard() {
        return DISCARD_POLICY;
    }

    public static RejectedExecutionHandler log() {
        return LOG_POLICY;
    }
}
