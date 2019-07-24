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

import java.util.concurrent.RejectedExecutionException;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/19 23:56
 * github - https://github.com/hl845740757
 */
public class RejectedExecutionHandlers {

    private static final RejectedExecutionHandler REJECT = new RejectedExecutionHandler() {
        @Override
        public void rejected(Runnable r, EventLoop executor) {
            throw new RejectedExecutionException();
        }
    };

    private static final RejectedExecutionHandler CALLER_RUNS = new RejectedExecutionHandler() {
        @Override
        public void rejected(Runnable r, EventLoop executor) {
            r.run();
        }
    };

    private RejectedExecutionHandlers() {

    }

    public static RejectedExecutionHandler reject() {
        return REJECT;
    }

    public static RejectedExecutionHandler callerRuns() {
        return CALLER_RUNS;
    }
}
