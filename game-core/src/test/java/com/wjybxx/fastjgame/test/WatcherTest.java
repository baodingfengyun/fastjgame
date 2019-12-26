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

package com.wjybxx.fastjgame.test;

import com.wjybxx.fastjgame.mgr.CuratorMgr;
import com.wjybxx.fastjgame.utils.ConcurrentUtils;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.data.Stat;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * watcher 测试，注册的时候节点存在于不存在的时候测试
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/2 10:02
 * github - https://github.com/hl845740757
 */
public class WatcherTest {

    private static final String path = "/watcher/checkExists";
    private static final CountDownLatch countDownLatch = new CountDownLatch(1);

    public static void main(String[] args) throws Exception {
        CuratorMgr curatorMgr = CuratorTest.newCuratorMgr();

        // checkExist 使用watcher之后
        // 1. 如果当前节点不存在，会在节点创建之后收到通知
        // 2. 如果节点存在，则会在节点删除之后收到通知
        Stat stat = curatorMgr.getClient().checkExists().usingWatcher((CuratorWatcher) WatcherTest::existCallBack).forPath(path);
        if (null == stat) {
            System.out.println("path " + path + " non-exist");
            curatorMgr.createNode(path, CreateMode.PERSISTENT, "checkExists".getBytes(StandardCharsets.UTF_8));
        } else {
            System.out.println("path " + path + " already exists");
            curatorMgr.delete(path);
        }

        // 等待回调完成
        countDownLatch.await();
    }

    private static void existCallBack(WatchedEvent event) {
        System.out.println("existCallBack eventPath = " + event.getPath() + ", eventType = " + event.getType());
        countDownLatch.countDown();
    }
}
