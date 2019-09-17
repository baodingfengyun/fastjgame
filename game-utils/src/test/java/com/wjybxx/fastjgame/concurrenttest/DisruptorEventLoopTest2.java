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

package com.wjybxx.fastjgame.concurrenttest;

import com.wjybxx.fastjgame.concurrent.DefaultThreadFactory;
import com.wjybxx.fastjgame.concurrent.RejectedExecutionHandler;
import com.wjybxx.fastjgame.concurrent.RejectedExecutionHandlers;
import com.wjybxx.fastjgame.concurrent.disruptor.DisruptorEventLoop;
import com.wjybxx.fastjgame.utils.ConcurrentUtils;

import javax.annotation.Nonnull;
import java.util.concurrent.ThreadFactory;

/**
 * 测试启动时抛出异常是否会走到{@link #clean()}
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/9/16
 * github - https://github.com/hl845740757
 */
public class DisruptorEventLoopTest2 extends DisruptorEventLoop {

    public DisruptorEventLoopTest2(@Nonnull ThreadFactory threadFactory, @Nonnull RejectedExecutionHandler rejectedExecutionHandler) {
        super(null, threadFactory, rejectedExecutionHandler);
    }

    @Override
    protected void init() throws Exception {
        throw new RuntimeException();
    }

    @Override
    protected void loopOnce() throws Exception{
        super.loopOnce();
    }

    @Override
    protected void clean() throws Exception {
        super.clean();
    }

    public static void main(String[] args) {
        final DisruptorEventLoopTest2 eventLoops = new DisruptorEventLoopTest2(new DefaultThreadFactory("TEST"), RejectedExecutionHandlers.abort());
        eventLoops.execute(ConcurrentUtils.NO_OP_TASK);
    }
}
