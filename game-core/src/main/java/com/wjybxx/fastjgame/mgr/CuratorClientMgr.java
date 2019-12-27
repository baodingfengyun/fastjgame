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

package com.wjybxx.fastjgame.mgr;

import com.google.inject.Inject;
import com.wjybxx.fastjgame.annotation.EventLoopGroupSingleton;
import com.wjybxx.fastjgame.concurrent.DefaultThreadFactory;
import com.wjybxx.fastjgame.misc.BackoffRetryForever;
import com.wjybxx.fastjgame.utils.ConcurrentUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.EnsureContainers;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.utils.CloseableExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class CuratorClientMgr {

    private static final Logger logger = LoggerFactory.getLogger(CuratorClientMgr.class);

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
    public CuratorClientMgr(GameConfigMgr gameConfigMgr) throws InterruptedException {
        client = newStartedClient(gameConfigMgr);

        // 该线程池不要共享的好，它必须是单线程的，如果放在外部容易出问题
        backgroundExecutor = new ThreadPoolExecutor(1, 1,
                15, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                new DefaultThreadFactory("CURATOR-BACKGROUND"));
        // 后台事件不多，允许自动关闭
        backgroundExecutor.allowCoreThreadTimeOut(true);
    }

    public void shutdown() {
        ConcurrentUtils.safeExecute(client::close);
        backgroundExecutor.shutdownNow();
        logger.info("CuratorClientMgr shutdown success");
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
    private static CuratorFramework newStartedClient(GameConfigMgr gameConfigMgr) throws InterruptedException {
        // WTF ??? CURATOR内部居然使用的是守护线程，如果要绑定线程工厂一定要注意，否则可能导致JVM无法退出。
        final CuratorFramework framework = CuratorFrameworkFactory.builder()
                .namespace(gameConfigMgr.getZkNameSpace())
                .connectString(gameConfigMgr.getZkConnectString())
                .connectionTimeoutMs(gameConfigMgr.getZkConnectionTimeoutMs())
                .sessionTimeoutMs(gameConfigMgr.getZkSessionTimeoutMs())
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
     * 默认时间是很难调整和确定的。
     *
     * @return RetryPolicy
     */
    public static BackoffRetryForever newForeverRetry() {
        return newForeverRetry(200, 5000);
    }

    /**
     * 使用指定参数创建一个带退避算法的永久尝试策略
     *
     * @param baseSleepTimeMs 起始等待时间(最小等待时间)(毫秒)
     * @param maxSleepTimeMs  最大等待时间(毫秒)
     * @return RetryPolicy
     */
    public static BackoffRetryForever newForeverRetry(int baseSleepTimeMs, int maxSleepTimeMs) {
        return new BackoffRetryForever(baseSleepTimeMs, maxSleepTimeMs);
    }

    /**
     * 创建一个用于监听{@link PathChildrenCache}事件和拉取数据的executor
     */
    public CloseableExecutorService newClosableExecutorService() {
        return new CloseableExecutorService(backgroundExecutor, false);
    }
}
