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
import com.wjybxx.fastjgame.misc.BackoffRetryForever;
import com.wjybxx.fastjgame.utils.ConcurrentUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.EnsureContainers;
import org.apache.curator.utils.CloseableExecutorService;

import javax.annotation.concurrent.ThreadSafe;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 管理curator的全局客户端
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/4
 * github - https://github.com/hl845740757
 */
@EventLoopGroupSingleton
@ThreadSafe
public class CuratorClientMrg {

    /**
     * CuratorFramework instances are fully thread-safe.
     * You should share one CuratorFramework per ZooKeeper cluster in your application.
     * <p>
     * 说实话：curator的代码我看过部分，敢说线程安全，还是有点怀疑，看的越多越不信.....
     * eg：{@link EnsureContainers#ensure()}就有很严重的竞争问题，用{@link AtomicBoolean}没啥意义...
     */
    private final CuratorFramework client;

    /**
     * 用于curator后台拉取数据的执行线程;
     * 不可共享，因为它必须是单线程的，以保持事件顺序。
     * 如果共享，如果有人修改线程数将出现问题。
     */
    private final ThreadPoolExecutor backgroundExecutor;

    @Inject
    public CuratorClientMrg(GameConfigMrg gameConfigMrg) throws InterruptedException {
        client = newStartedClient(gameConfigMrg);

        // 该线程池不要共享的好，它必须是单线程的，如果放在外部容易出问题
        backgroundExecutor = new ThreadPoolExecutor(1, 1,
                5, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                new DefaultThreadFactory("curator-backround"));
    }

    public void shutdown() {
        ConcurrentUtils.safeExecute((Runnable) client::close);
        backgroundExecutor.shutdownNow();
    }

    /**
     * 获取curator的全局客户端
     */
    public CuratorFramework getClient() {
        return client;
    }

    /**
     * 为外部提供创建client的工厂方法。
     * 注意：创建的client在使用完之后必须调用{@link CuratorFramework#close()}关闭
     *
     * @return 已调用启动方法的客户端
     */
    private CuratorFramework newStartedClient(GameConfigMrg gameConfigMrg) throws InterruptedException {
        CuratorFramework framework = CuratorFrameworkFactory.builder()
                .namespace(gameConfigMrg.getZkNameSpace())
                .connectString(gameConfigMrg.getZkConnectString())
                .connectionTimeoutMs(gameConfigMrg.getZkConnectionTimeoutMs())
                .sessionTimeoutMs(gameConfigMrg.getZkSessionTimeoutMs())
                .retryPolicy(newForeverRetry())
                .build();
        framework.start();
        framework.blockUntilConnected();
        return framework;
    }

    /**
     * 使用默认的参数创建一个带退避算法的永久尝试策略。
     * 默认最小等待时间200ms;
     * 默认最大等待时间5s;
     *
     * @return RetryPolicy
     */
    public BackoffRetryForever newForeverRetry() {
        // 50ms - 3s 默认时间是很难调整和确定的
        return newForeverRetry(200, 5000);
    }

    /**
     * 使用指定参数创建一个带退避算法的永久尝试策略
     *
     * @param baseSleepTimeMs 起始等待时间(最小等待时间)(毫秒)
     * @param maxSleepTimeMs  最大等待时间(毫秒)
     * @return RetryPolicy
     */
    public BackoffRetryForever newForeverRetry(int baseSleepTimeMs, int maxSleepTimeMs) {
        return new BackoffRetryForever(baseSleepTimeMs, maxSleepTimeMs);
    }

    public CloseableExecutorService newClosableExecutorService() {
        return new CloseableExecutorService(backgroundExecutor, false);
    }
}
