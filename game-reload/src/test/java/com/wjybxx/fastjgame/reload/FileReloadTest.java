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

package com.wjybxx.fastjgame.reload;

import com.wjybxx.fastjgame.reload.file.WhiteListReader;
import com.wjybxx.fastjgame.reload.mgr.FileReloadMgr;
import com.wjybxx.fastjgame.reload.mgr.ScanResult;
import com.wjybxx.fastjgame.util.concurrent.DefaultThreadFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/11/30
 * github - https://github.com/hl845740757
 */
public class FileReloadTest {

    public static final String PROJECT_RES_DIR = "./res";

    public static void main(String[] args) throws Exception {
        initFile();

        final ReloadTestDataMgr testDataMgr = new ReloadTestDataMgr();
        final FileReloadMgr fileReloadMgr = new FileReloadMgr(PROJECT_RES_DIR, testDataMgr, newThreadPool());
        final ScanResult scanResult = ScanResult.valueOf(Set.of("com.wjybxx.fastjgame.reload.file"));
        fileReloadMgr.registerReaders(scanResult.fileReaders);
        fileReloadMgr.registerCacheBuilders(scanResult.fileCacheBuilders);

        // 准备就绪后加载文件
        fileReloadMgr.loadAll();
        System.out.println(testDataMgr.whiteList);

        changeFile();

        // 热更新
        fileReloadMgr.reloadAll();
        System.out.println(testDataMgr.whiteList);
    }

    private static void initFile() throws IOException {
        final String relativePath = WhiteListReader.FILE_NAME.getRelativePath();
        final File file = new File(PROJECT_RES_DIR + "/" + relativePath);
        if (file.exists()) {
            file.delete();
            file.createNewFile();
        }
    }

    private static void changeFile() throws Exception {
        final List<String> whiteList = List.of("wjybxx", "qswy");
        final String relativePath = WhiteListReader.FILE_NAME.getRelativePath();
        final File file = new File(PROJECT_RES_DIR + "/" + relativePath);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for (int i = 0; i < whiteList.size(); i++) {
                String account = whiteList.get(i);
                writer.write(account);
                if (i < whiteList.size() - 1) {
                    writer.newLine();
                }
            }
        }
    }

    public static Executor newThreadPool() {
        final int poolSize = 4;
        final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(poolSize, poolSize,
                1, TimeUnit.MINUTES,
                new ArrayBlockingQueue<>(poolSize * 64),
                new DefaultThreadFactory("COMMON_POOL", true),
                new ThreadPoolExecutor.CallerRunsPolicy());
        threadPoolExecutor.allowCoreThreadTimeOut(true);
        return threadPoolExecutor;
    }
}
