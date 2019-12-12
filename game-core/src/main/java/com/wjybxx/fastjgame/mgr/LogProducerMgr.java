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
import com.wjybxx.fastjgame.log.LogBuilder;
import com.wjybxx.fastjgame.log.LogProducerEventLoop;
import com.wjybxx.fastjgame.utils.ConcurrentUtils;

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

    private final LogProducerEventLoop producer;

    @Inject
    public LogProducerMgr(GameConfigMgr gameConfigMgr) {
        producer = new LogProducerEventLoop(gameConfigMgr.getKafkaBrokerList(),
                new DefaultThreadFactory("LOGGER"),
                RejectedExecutionHandlers.log());
    }

    public void start() {
        producer.execute(ConcurrentUtils.NO_OP_TASK);
    }

    public void shutdown() {
        producer.shutdown();
    }

    public void publish(LogBuilder logBuilder) {
        producer.publish(logBuilder);
    }
}
