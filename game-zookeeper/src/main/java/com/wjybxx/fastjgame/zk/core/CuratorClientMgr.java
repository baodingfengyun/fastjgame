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

package com.wjybxx.fastjgame.zk.core;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.wjybxx.fastjgame.utils.CloseableUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.EnsureContainers;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.utils.CloseableExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.ThreadSafe;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 管理curator的全局客户端。
 * 用户必须在最后调用{@link #shutdown()}关闭创建的资源。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/4
 * github - https://github.com/hl845740757
 */
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

    public CuratorClientMgr(final CuratorFrameworkFactory.Builder builder,
                            final ThreadFactory backgroundThreadFactory) throws InterruptedException {
        this.client = newStartedClient(builder);

        // 该线程池不要共享的好，它必须是单线程的，如果放在外部容易出问题
        backgroundExecutor = new ThreadPoolExecutor(1, 1,
                15, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                backgroundThreadFactory);

        // 后台事件不多，允许自动关闭
        backgroundExecutor.allowCoreThreadTimeOut(true);
    }

    private static CuratorFramework newStartedClient(final CuratorFrameworkFactory.Builder builder) throws InterruptedException {
        checkThreadFactory(builder);

        final CuratorFramework client = builder.build();
        client.start();
        client.blockUntilConnected();
        return client;
    }

    private static void checkThreadFactory(CuratorFrameworkFactory.Builder builder) {
        if (builder.getThreadFactory() == null) {
            return;
        }

        // Curator有一点非常坑爹
        // 内部使用的是守护线程，如果用户指定了线程工厂，设置错误的话，则可能导致JVM无法退出。
        // 我们在此拦截，以保证安全性
        final ThreadFactory daemonThreadFactory = new ThreadFactoryBuilder()
                .setThreadFactory(builder.getThreadFactory())
                .setDaemon(true)
                .build();

        builder.threadFactory(daemonThreadFactory);
    }

    public void shutdown() {
        CloseableUtils.closeQuietly(client);
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
     * 创建一个用于监听{@link PathChildrenCache}事件和拉取数据的executor
     */
    public CloseableExecutorService newClosableExecutorService() {
        return new CloseableExecutorService(backgroundExecutor, false);
    }

}
