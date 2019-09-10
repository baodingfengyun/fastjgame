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
import com.wjybxx.fastjgame.annotation.EventLoopSingleton;
import com.wjybxx.fastjgame.misc.LockPathAction;
import com.wjybxx.fastjgame.misc.ObjectHolder;
import com.wjybxx.fastjgame.misc.ResourceCloseHandle;
import com.wjybxx.fastjgame.utils.*;
import com.wjybxx.fastjgame.world.GameEventLoopMrg;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.barriers.DistributedBarrier;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.utils.CloseableExecutorService;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 2019年8月6日 归入线程级别管理器。
 *
 * <h3>Curator</h3>
 * 官网：http://curator.apache.org/getting-started.html<br>
 * 使用zookeeper建议配合ZKUI。
 *
 * <h3>ZKUI</h3>
 * ZKUI是我常用的zookeeper图形化界面工具，我自己的版本修正了原始版本的中文乱码问题(配置支持中文会好很多)，
 * 和根节点属性导出之后无法导入的问题。
 * 地址： - https://github.com/hl845740757/zkui
 *
 * <P>
 * Curator控制器，管理Curator客户端和提供一些简便方法。
 * 设计给游戏逻辑线程使用，因此不是线程安全的。
 * </P>
 *
 * <h3>注意事项</h3>
 * <P>
 * <li>1.如果操作的是私有数据，则先检查后执行是安全的。</li>
 * <li>2.如果操作的是共享数据，则可能导致异常，需要加锁。</li>
 * <li>3.对永久节点的操作，加锁可保证先检查后执行的原子性(防止并发修改)。</li>
 * <li>4.对于临时节点，即使加锁也不能保证绝对的安全性，因为临时节点的删除是自动的，即使检查到(不)存在，下一步操作仍然可能失败。</li>
 * <li>5.对于临时节点的操作一定要小心，建议使用watcher获取数据</li>
 * </P>
 * <p>
 * 加锁时注意：不可以对临时节点加锁(临时节点不能创建子节点，你需要使用另外一个节点加锁，来保护它)。
 * {@link ZKPathUtils#findAppropriateLockPath(String)}可能会有帮助。
 * </p>
 * 警告：
 * 关于Curator的{@link NodeCache} 和 {@link PathChildrenCache}线程安全问题请查看笔记：
 * - http://note.youdao.com/noteshare?id=721ba3029455fac81d8ec19c813423bf&sub=D20C495A90CD4487A909EE6637A788A6
 *
 * <p>
 * 如果提供的简单方法不能满足需要，可以调用{@link #getClient()}获取client。
 * </p>
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/12 12:05
 * github - https://github.com/hl845740757
 */
@EventLoopSingleton
@NotThreadSafe
public class CuratorMrg {

    private static final Logger logger = LoggerFactory.getLogger(CuratorMrg.class);

    /**
     * 每个路径的锁信息
     */
    private final Map<String, InterProcessMutex> lockMap = new HashMap<>();
    /**
     * 已分配的节点缓存，以方便统一关闭（避免使用者忘记关闭之后导致泄漏）
     */
    private final List<PathChildrenCache> allocateNodeCache = new ArrayList<>(10);

    private final CuratorClientMrg clientMrg;
    private final CuratorFramework client;
    private final GameEventLoopMrg gameEventLoopMrg;

    @Inject
    public CuratorMrg(CuratorClientMrg curatorClientMrg, GameEventLoopMrg gameEventLoopMrg) {
        this.clientMrg = curatorClientMrg;
        this.client = curatorClientMrg.getClient();
        this.gameEventLoopMrg = gameEventLoopMrg;
    }

    /**
     * 当你有更加复杂的需求时，获取客户端可能有帮助
     */
    public CuratorFramework getClient() {
        return client;
    }


    public void shutdown() {
        CollectionUtils.removeIfAndThen(allocateNodeCache, FunctionUtils::TRUE, GameUtils::closeQuietly);
    }

    /**
     * 对某个永久类型节点加锁，不可以直接对临时节点加锁(临时节点无法创建子节点)，可锁其父节点或其它永久节点；
     * {@link ZKPathUtils#findAppropriateLockPath(String)}可能有帮助
     * <p>
     * 实现：阻塞直到锁可用，锁为可重入锁。每一次lock调用都必须有一次对应的{@link #unlock(String)}}调用。
     * 食用方式，就向使用jdk的显式锁一样：
     *
     * <pre>
     *     {@code
     *         curatorMrg.lock(path);
     *         try{
     *             // do something
     *         } finally{
     *             curatorMrg.unlock(path);
     *         }
     *     }
     * </pre>
     * 也可以使用{@link #actionWhitLock(String, LockPathAction)}
     *
     * @param path 请求加锁的路径，再次提醒：该节点不可以是临时节点。
     * @throws Exception ZK errors, connection interruptions
     */
    public void lock(String path) throws Exception {
        // 需要保留下来才能重入
        InterProcessMutex lock = lockMap.computeIfAbsent(path, key -> new InterProcessMutex(client, key));
        lock.acquire();
    }

    /**
     * 释放对应路径的锁
     *
     * @param path 路径
     */
    public void unlock(String path) throws Exception {
        InterProcessMutex lock = lockMap.get(path);
        if (null == lock) {
            throw new IllegalStateException("path " + path + " lock state is wrong.");
        }
        lock.release();
    }

    /**
     * 如果加锁成功，则执行后续逻辑，采用这种方式可以简化外部代码结构，且更加安全。
     *
     * @param lockPath 要锁住的节点
     * @param action   执行什么操作
     */
    public void actionWhitLock(String lockPath, LockPathAction action) throws Exception {
        lock(lockPath);
        try {
            action.doAction();
        } finally {
            unlock(lockPath);
        }
    }

    // region 其它辅助方法

    /**
     * 判断路径是否存在，建议只使用在永久类型节点上。
     * 对于分布式下的并发操作，如果先检查后执行逻辑，必须先持有保护该路径数据的锁。
     * <p>
     * 如果仅仅是期望节点存在时获取数据，使用原子的{@link #getDataIfPresent(String)}更加安全。
     *
     * @param path 路径
     * @return 如果存在则返回true，否则返回false
     * @throws Exception ZK errors, connection interruptions
     */
    public boolean isPathExist(String path) throws Exception {
        return null != client.checkExists().forPath(path);
    }

    /**
     * 尝试获取对应节点的数据，建议使用在永久节点上，如果是可能并发修改的节点，必须加锁。
     * 对于永久节点，加锁可保证先检查后执行复合操作的原子性。
     * 对于临时节点，即使外部加锁，也无法保证检查到路径存在时一定能获取到数据。
     * 主要原因是临时节点超时删除的无法控制的。
     * eg.
     * 加锁 -> 检查到临时节点存在 -> (远程自动删除节点) -> 获取数据失败
     * <p>
     * 因此该方法适用于永久节点，对于临时节点千万不要先检查后执行，临时节点请使用
     * {@link #getDataIfPresent(String)}，根据获取的结果判断。
     *
     * @param path 路径
     * @return 如果路径存在，则返回对应的数据，否则抛出异常
     * @throws Exception 节点不存在抛出异常，以及zookeeper连接断开导致的异常
     */
    public byte[] getData(String path) throws Exception {
        return client.getData().forPath(path);
    }

    /**
     * 当节点存在时返回节点的数据，否则返回null，它是一个原子操作。
     * 它不是一个先检查后执行的复合操作。
     * <b>获取临时节点数据必须使用该方法。</b>
     *
     * @param path 节点路径
     * @return 节点数据
     * @throws Exception zk errors.
     */
    @Nullable
    public byte[] getDataIfPresent(String path) throws Exception {
        try {
            return client.getData().forPath(path);
        } catch (KeeperException.NoNodeException ignore) {
            // ignore,may other process delete this node, it's ok
        }
        return null;
    }

    /**
     * 创建一个空节点，如果是并发创建的节点，注意加锁，
     * (可能存在检测到节点不存在，创建节点仍可能失败)，期望原子的操作请使用
     * {@link #createNodeIfAbsent(String, CreateMode)} 和
     * {@link #createNodeIfAbsent(String, CreateMode, byte[])}
     *
     * @param path 路径
     * @param mode 模式
     * @return 返回创建的路径
     * @throws Exception 节点存在抛出异常，以及zookeeper连接断开导致的异常
     */
    public String createNode(String path, CreateMode mode) throws Exception {
        return client.create().creatingParentsIfNeeded().withMode(mode).forPath(path);
    }

    /**
     * 创建一个节点，并以指定数据初始化它，如果是并发创建的节点，注意加锁，
     * (可能存在检测到节点不存在，创建节点仍可能失败)，期望原子的操作请使用
     * {@link #createNodeIfAbsent(String, CreateMode)} 和
     * {@link #createNodeIfAbsent(String, CreateMode, byte[])}
     *
     * @param path     路径
     * @param mode     模式
     * @param initData 初始数据
     * @return 返回创建的路径
     * @throws Exception 节点存在抛出异常，以及zookeeper连接断开导致的异常
     */
    public String createNode(String path, CreateMode mode, @Nonnull byte[] initData) throws Exception {
        return client.create().creatingParentsIfNeeded().withMode(mode).forPath(path, initData);
    }

    /**
     * 如果节点不存在的话，创建它。它是一个原子操作，不是先检查后执行的操作
     *
     * @param path 路径
     * @param mode 模式
     * @return 创建成功则返回true，否则返回false
     * @throws Exception zk errors
     */
    public boolean createNodeIfAbsent(String path, CreateMode mode) throws Exception {
        try {
            createNode(path, mode);
            return true;
        } catch (KeeperException.NodeExistsException ignore) {
            // ignore
        }
        return false;
    }

    /**
     * 如果节点不存在的话创建一个节点，并以指定数据初始化它。
     * 它是一个原子操作，不是一个先检查后执行的复合操作。
     *
     * @param path     路径
     * @param mode     模式
     * @param initData 初始数据
     * @return 成功创建则返回true，否则返回false
     * @throws Exception zk errors
     */
    public boolean createNodeIfAbsent(String path, CreateMode mode, @Nonnull byte[] initData) throws Exception {
        try {
            createNode(path, mode, initData);
            return true;
        } catch (KeeperException.NodeExistsException ignore) {
            // ignore 等价于cas尝试失败
        }
        return false;
    }

    /**
     * 设置某个节点的数据，如果是并发更新的节点，一定要注意加锁！
     *
     * @param path 路径
     * @param data 数据
     * @return 节点的最新状态
     * @throws Exception 节点不存在抛出异常，以及zookeeper连接断开导致的异常
     */
    public Stat setData(String path, byte[] data) throws Exception {
        return client.setData().forPath(path, data);
    }

    /**
     * 删除一个节点，节点不存在时什么也不做;
     * 单个节点的删除不需要加锁；尽量不要手动删临时节点；
     *
     * @param path 路径。
     * @throws Exception zookeeper连接断开导致的异常
     */
    public void delete(String path) throws Exception {
        try {
            client.delete().deletingChildrenIfNeeded().forPath(path);
        } catch (KeeperException.NoNodeException e) {
            // ignore 没有影响，别人帮助我完成了这件事
        }
    }

    /**
     * 获取某个节点的所有子节点属性
     *
     * @param path 节点路径
     * @return 所有的子节点路径, 如果该节点不存在或没有子节点，则返回emptyList
     * @throws Exception zk errors
     */
    public List<String> getChildren(String path) throws Exception {
        try {
            return client.getChildren().forPath(path);
        } catch (KeeperException.NoNodeException e) {
            // ignore 没有影响
            return Collections.emptyList();
        }
    }

    /**
     * 获取当前节点的所有子节点数据，返回的只是个快照。
     * 你可以参考{@link PathChildrenCache#rebuild()}
     *
     * @param path 父节点路径
     * @return childFullPath -> data
     */
    public Map<String, byte[]> getChildrenData(String path) throws Exception {
        List<String> children = getChildren(path);
        Map<String, byte[]> result = new LinkedHashMap<>();
        for (String child : children) {
            String childFullPath = ZKPaths.makePath(path, child);
            byte[] childData = getDataIfPresent(childFullPath);
            // 即使 getChildren 查询到节点存在，也不一定能获取到数据，一定要注意
            if (null != childData) {
                result.put(childFullPath, childData);
            }
        }
        return result;
    }

    /**
     * 创建一个路径节点缓存，返回之前已调用{@link PathChildrenCache#start()}，
     * 你需要在使用完之后调用{@link PathChildrenCache#close()}关闭缓存节点；
     * 如果你忘记关闭，那么会在curator关闭的时候统一关闭。
     * 更多线程安全问题，请查看类文档中提到的笔记。
     *
     * @param path     父节点
     * @param listener 缓存事件监听器，运行在逻辑线程，不必考虑线程安全。
     * @return PathChildrenCache 不再使用时需要手动关闭！不要使用{@link PathChildrenCache}获取数据
     * @throws Exception zk errors
     */
    public ResourceCloseHandle watchChildren(String path, @Nonnull PathChildrenCacheListener listener) throws Exception {
        // CloseableExecutorService这个还是不共享的好
        CloseableExecutorService watcherService = clientMrg.newClosableExecutorService();
        // 指定pathChildrenCache接收事件的线程，复用线程池，以节省开销。
        PathChildrenCache pathChildrenCache = new PathChildrenCache(client, path, true, false, watcherService);

        // 先添加listener以确保不会遗漏事件 --- 使用EventLoop线程监听，消除同步，listener不必考虑线程安全问题。
        pathChildrenCache.getListenable().addListener(listener, gameEventLoopMrg.getEventLoop());
        // 避免外部忘记关闭
        this.allocateNodeCache.add(pathChildrenCache);

        // 启动缓存
        pathChildrenCache.start(PathChildrenCache.StartMode.NORMAL);
        return new ResourceCloseHandle(pathChildrenCache);
    }

    /**
     * 等待节点出现。
     *
     * @param path 节点路径
     * @return 节点的数据
     */
    public byte[] waitForNodeCreate(final String path) throws Exception {
        // 使用NodeCache的话，写了太多代码，搞得复杂了，不利于维护，使用简单的轮询代替。
        // 轮询虽然不雅观，但是正确性易保证
        ObjectHolder<byte[]> resultHolder = new ObjectHolder<>();
        ConcurrentUtils.awaitRemoteWithSleepingRetry(() -> {
            resultHolder.setValue(getDataIfPresent(path));
            return resultHolder.getValue() != null;
        }, 1, TimeUnit.SECONDS);
        return resultHolder.getValue();
    }

    /**
     * 等待节点删除，当节点不存在，立即返回（注意先检查后执行的原子性问题）。
     *
     * @param path 节点路径
     * @throws Exception zk errors
     */
    public void waitForNodeDelete(String path) throws Exception {
        final DistributedBarrier barrier = new DistributedBarrier(client, path);
        ConcurrentUtils.awaitRemoteUninterruptibly(barrier::waitOnBarrier);
    }

    /**
     * 删除指定节点，如果该节点没有子节点的话。
     * (该操作是一个复合操作，注意加锁)
     *
     * @param path 路径
     */
    public void deleteNodeIfNoChild(String path) throws Exception {
        List<String> children = getChildren(path);
        if (children.size() == 0) {
            delete(path);
        }
    }

    // endregion
}
