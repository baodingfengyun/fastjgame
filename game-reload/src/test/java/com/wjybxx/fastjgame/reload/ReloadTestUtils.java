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

package com.wjybxx.fastjgame.reload;

import com.wjybxx.fastjgame.util.concurrent.DefaultThreadFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author wjybxx
 * date - 2020/12/3
 * github - https://github.com/hl845740757
 */
public class ReloadTestUtils {

    public static final String PROJECT_RES_DIR = "./res";

    static void recreateFile(String relativePath) throws IOException {
        final File file = new File(PROJECT_RES_DIR + "/" + relativePath);
        if (file.exists()) {
            file.delete();
        }
        file.createNewFile();
    }

    static void writeStringList(String relativePath, List<String> whiteList) throws IOException {
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

    static Executor newThreadPool() {
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
