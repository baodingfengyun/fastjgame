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

import com.wjybxx.fastjgame.concurrent.DefaultThreadFactory;
import com.wjybxx.fastjgame.zk.core.BackoffRetryForever;
import com.wjybxx.fastjgame.zk.core.CuratorClientMgr;
import com.wjybxx.fastjgame.zk.guid.ZKGuidGenerator;
import org.apache.curator.framework.CuratorFrameworkFactory;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/12
 * github - https://github.com/hl845740757
 */
public class ZKGuidGeneratorTest {

    public static void main(String[] args) throws InterruptedException {
        final CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder()
                .namespace("test")
                .connectString("127.0.0.1:2181")
                .connectionTimeoutMs(30 * 1000)
                .sessionTimeoutMs(30 * 1000)
                .retryPolicy(new BackoffRetryForever());


        final CuratorClientMgr curatorClientMgr = new CuratorClientMgr(builder, new DefaultThreadFactory("CURATOR_BACKGROUD"));
        try {
            doTest(curatorClientMgr, "playerGuidGenerator");
            doTest(curatorClientMgr, "monsterGuidGenerator");
        } finally {
            curatorClientMgr.shutdown();
        }
    }

    private static void doTest(CuratorClientMgr curatorClientMgr, String name) {
        final int cacheSize = 100;
        final ZKGuidGenerator guidGenerator = new ZKGuidGenerator(curatorClientMgr, name, cacheSize);
        try {
            for (int index = 0; index < cacheSize * 3; index++) {
                System.out.println("name: " + name + ", guid: " + guidGenerator.next());
            }
        } finally {
            guidGenerator.close();
        }
    }

}
