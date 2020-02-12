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

import com.wjybxx.fastjgame.utils.CodecUtils;
import com.wjybxx.fastjgame.utils.ZKPathUtils;
import org.apache.curator.framework.recipes.atomic.DistributedAtomicLong;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.zookeeper.CreateMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class ZKGuidGenerator implements GuidGenerator {

    private static final Logger logger = LoggerFactory.getLogger(ZKGuidGenerator.class);

    private final String name;
    private final CuratorFacade curatorFacade;

    /**
     * guid区间索引
     * (32位好像有点多哈)
     */
    private int guidIndex = 0;
    /**
     * guid本地缓存
     */
    private int guidSequence = 0;

    public ZKGuidGenerator(final String name, CuratorClientMgr clientMgr) {
        this.name = name;
        this.curatorFacade = clientMgr.getFacade();
    }


    @Override
    public String name() {
        return name;
    }

    @Override
    public long next() {
        try {
            checkCache();
            // 这种方式的guid不是很方便查看规律(如果是乘以10的N次方可能方便查看使用情况)
            long high = ((long) guidIndex) << 32;
            int low = guidSequence++;
            return high + low;
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
        // 还未初始化
        if (guidIndex == 0) {
            init();
            return;
        }
        // 本地缓存用完了
        if (guidSequence == Integer.MAX_VALUE) {
            final String lockPath = getGuidLockPath();
            curatorFacade.actionWhitLock(lockPath, this::incGuidIndex);
        }
    }

    /**
     * 缓存真正更新的地方
     *
     * @param guidIndex 新的guid区间
     */
    private void updateCache(int guidIndex) {
        this.guidIndex = guidIndex;
        this.guidSequence = 1;
        logger.info("guidIndex={}", guidIndex);
    }

    /**
     * 完成guid缓存区间的初始化
     *
     * @throws Exception zk errors
     */
    private void init() throws Exception {
        final String guidIndexPath = getGuidPath();
        final String lockPath = getGuidLockPath();

        curatorFacade.actionWhitLock(lockPath, () -> {
            if (!curatorFacade.isPathExist(guidIndexPath)) {
                // 初始化为1 并据为己有，序列化为字符串字节数组具有更好的可读性
                final byte[] initData = CodecUtils.getBytesUTF8("1");
                curatorFacade.createNode(guidIndexPath, CreateMode.PERSISTENT, initData);
                updateCache(1);
            } else {
                incGuidIndex();
            }
        });
    }

    private String getGuidPath() {
        return ZKPathUtils.makePath(ZKPathUtils.GUID_PATH_ROOT, name);
    }

    private String getGuidLockPath() {
        return ZKPathUtils.makePath(ZKPathUtils.GUID_PATH_ROOT, name + "_lock");
    }

    /**
     * 更新本地guid缓存，需要运行在锁保护下
     *
     * @throws Exception zk errors
     */
    private void incGuidIndex() throws Exception {
        final byte[] oldData = curatorFacade.getData(getGuidPath());
        final int oldIndex = parseIntFromStringBytes(oldData);

        final int nextGuidIndex = oldIndex + 1;
        byte[] newData = serializeToStringBytes(nextGuidIndex);

        curatorFacade.setData(getGuidPath(), newData);

        updateCache(nextGuidIndex);
    }

    private byte[] serializeToStringBytes(int nextGuidIndex) {
        return CodecUtils.getBytesUTF8("" + nextGuidIndex);
    }

    private int parseIntFromStringBytes(byte[] oldData) {
        return Integer.parseInt(CodecUtils.newStringUTF8(oldData));
    }

}
