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

package com.wjybxx.fastjgame.mrg;

import com.google.inject.Inject;
import com.wjybxx.fastjgame.annotation.EventLoopGroupSingleton;
import com.wjybxx.fastjgame.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 全局的线程池管理器，用于执行一些不是那么大型的任务。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/10 0:34
 * github - https://github.com/hl845740757
 */
@EventLoopGroupSingleton
@ThreadSafe
public class GlobalExecutorMrg {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExecutorMrg.class);

    private final ThreadPoolExecutor executorService;

    @Inject
    public GlobalExecutorMrg(GameConfigMrg gameConfigMrg) {
        // 最多创建配置个数的线程
        executorService = new ThreadPoolExecutor(1, gameConfigMrg.getGlobalExecutorThreadNum(),
                5, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                new DefaultThreadFactory("global_executor"));
    }

    @Nonnull
    public ExecutorService getExecutorService() {
        return executorService;
    }

    public void shutdown() {
        executorService.shutdownNow();
        logger.info("GlobalExecutorMrg shutdown success.");
    }

}
