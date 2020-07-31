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

package com.wjybxx.fastjgame.net.eventloop;

import com.wjybxx.fastjgame.utils.CheckUtils;
import com.wjybxx.fastjgame.utils.concurrent.DefaultThreadFactory;
import com.wjybxx.fastjgame.utils.concurrent.RejectedExecutionHandler;
import com.wjybxx.fastjgame.utils.concurrent.RejectedExecutionHandlers;

import javax.annotation.Nonnull;
import java.util.concurrent.ThreadFactory;

public class NetEventLoopGroupBuilder {

    /**
     * 设置{@link NetEventLoopGroup}中的{@link NetEventLoop}数量，如果session数较多，建议多个。
     */
    private int netEventLoopNum = 1;
    /**
     * netty线程配置
     */
    private int bossGroupThreadNum = 1;
    private int workerGroupThreadNum = 8;

    private ThreadFactory threadFactory = new DefaultThreadFactory("NetEventLoop");
    private RejectedExecutionHandler rejectedExecutionHandler = RejectedExecutionHandlers.abort();

    public NetEventLoopGroupBuilder setNetEventLoopNum(int netEventLoopNum) {
        this.netEventLoopNum = CheckUtils.requirePositive(netEventLoopNum, "netEventLoopNum");
        return this;
    }

    public NetEventLoopGroupBuilder setBossGroupThreadNum(int bossGroupThreadNum) {
        this.bossGroupThreadNum = CheckUtils.requirePositive(bossGroupThreadNum, "bossGroupThreadNum");
        return this;
    }

    public NetEventLoopGroupBuilder setWorkerGroupThreadNum(int workerGroupThreadNum) {
        this.workerGroupThreadNum = CheckUtils.requirePositive(workerGroupThreadNum, "workerGroupThreadNum");
        return this;
    }

    public NetEventLoopGroupBuilder setThreadFactory(@Nonnull ThreadFactory threadFactory) {
        this.threadFactory = threadFactory;
        return this;
    }

    public NetEventLoopGroupBuilder setRejectedExecutionHandler(@Nonnull RejectedExecutionHandler rejectedExecutionHandler) {
        this.rejectedExecutionHandler = rejectedExecutionHandler;
        return this;
    }

    public NetEventLoopGroupImp build() {
        final NetEventLoopGroupImp.GroupConfig groupConfig = new NetEventLoopGroupImp.GroupConfig(bossGroupThreadNum, workerGroupThreadNum);
        return new NetEventLoopGroupImp(netEventLoopNum, threadFactory, rejectedExecutionHandler, groupConfig);
    }
}