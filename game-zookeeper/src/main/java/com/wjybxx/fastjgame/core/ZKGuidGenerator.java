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
package com.wjybxx.fastjgame.core;

import com.wjybxx.fastjgame.concurrent.ImmediateEventLoop;
import com.wjybxx.fastjgame.utils.CodecUtils;
import com.wjybxx.fastjgame.utils.ZKPathUtils;
import org.apache.curator.framework.recipes.atomic.DistributedAtomicLong;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.zookeeper.CreateMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * 基于zookeeper实现的guid生成器。
 * <p>
 * guid生成器控制器，使用redis是最简单方便的。
 * 但是现在好像还没有必要引入redis，而zookeeper是必须引入的，因此暂时还是使用zookeeper实现；
 * 尝试过{@link DistributedAtomicLong}，但是确实有点复杂，最后还是使用了分布式锁{@link InterProcessMutex}。
 * <p>
 * 实现方式和我们项目中的一致，缓存的是整个int正整数区间，它的缺点是资源浪费(但是也更安全)，
 * 优点是，基本上进程运行期间只需要缓存一次，会减小出现错误的几率。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/12 11:59
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public class ZKGuidGenerator implements GuidGenerator {

    private static final Logger logger = LoggerFactory.getLogger(ZKGuidGenerator.class);
    private static final int DEFAULT_CACHE_SIZE = 1000_000;

    private final CuratorFacade curatorFacade;
    private final String name;
    private final int cacheSize;

    private long curGuid = 0;
    private long curBarrier = 0;

    public ZKGuidGenerator(CuratorClientMgr curatorClientMgr, final String name) {
        this(curatorClientMgr, name, DEFAULT_CACHE_SIZE);
    }

    public ZKGuidGenerator(CuratorClientMgr curatorClientMgr, final String name, final int cacheSize) {
        this.curatorFacade = new CuratorFacade(curatorClientMgr, ImmediateEventLoop.INSTANCE);
        this.name = name;
        this.cacheSize = cacheSize;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public long next() {
        try {
            checkCache();

            return curGuid++;
        } catch (Exception e) {
            throw new IllegalStateException("may lose zk connect", e);
        }
    }

    /**
     * 检查缓存是否需要更新
     *
     * @throws Exception zk error
     */
    private void checkCache() throws Exception {
        if (curGuid > 0 && curGuid <= curBarrier) {
            return;
        }

        // 本地缓存用完了
        final String lockPath = getLockPath();
        curatorFacade.actionWhitLock(lockPath, this::refreshCache);
    }

    /**
     * 更新本地guid缓存，需要运行在锁保护下
     *
     * @throws Exception zk errors
     */
    private void refreshCache() throws Exception {
        final String guidPath = getGuidPath();
        final byte[] curData = curatorFacade.getDataIfPresent(guidPath);
        final long currentValue = curData == null ? 0 : parseIntFromStringBytes(curData);

        this.curGuid = currentValue + 1;
        this.curBarrier = currentValue + cacheSize;

        final byte[] newData = serializeToStringBytes(curBarrier);
        if (null == curData) {
            curatorFacade.createNode(guidPath, CreateMode.PERSISTENT, newData);
        } else {
            curatorFacade.setData(guidPath, newData);
        }

        logger.info("update guid cache, curGuid={}, curBarrier={}", curGuid, curBarrier);
    }

    private String getGuidPath() {
        return ZKPathUtils.makePath(ZKPathUtils.GUID_PATH_ROOT, name);
    }

    private String getLockPath() {
        return ZKPathUtils.makePath(ZKPathUtils.GUID_PATH_ROOT, name + "_lock");
    }

    /**
     * 序列化为字符串字节数组具有更好的可读性
     */
    private static byte[] serializeToStringBytes(long guidIndex) {
        return CodecUtils.getBytesUTF8(Long.toString(guidIndex));
    }

    private static long parseIntFromStringBytes(byte[] guidData) {
        return Long.parseLong(CodecUtils.newStringUTF8(guidData));
    }

    @Override
    public void close() {
        curatorFacade.shutdown();
    }
}
