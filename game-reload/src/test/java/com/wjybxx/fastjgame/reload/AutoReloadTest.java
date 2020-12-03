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

import com.wjybxx.fastjgame.reload.file.BlackListReader;
import com.wjybxx.fastjgame.reload.mgr.FileReloadMgr;
import com.wjybxx.fastjgame.util.ThreadUtils;
import com.wjybxx.fastjgame.util.time.TimeProviders;
import com.wjybxx.fastjgame.util.time.TimeUtils;
import com.wjybxx.fastjgame.util.timer.DefaultTimerSystem;
import com.wjybxx.fastjgame.util.timer.TimerSystem;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.wjybxx.fastjgame.reload.ReloadTestUtils.PROJECT_RES_DIR;
import static com.wjybxx.fastjgame.reload.ReloadTestUtils.newThreadPool;

/**
 * @author wjybxx
 * date - 2020/12/3
 * github - https://github.com/hl845740757
 */
public class AutoReloadTest {

    public static void main(String[] args) throws Exception {
        initFile();

        final ReloadTestDataMgr testDataMgr = new ReloadTestDataMgr();
        final TimerSystem timerSystem = new DefaultTimerSystem(TimeProviders.realtimeProvider());
        final FileReloadMgr fileReloadMgr = new FileReloadMgr(PROJECT_RES_DIR, testDataMgr, timerSystem, newThreadPool(),
                5 * TimeUtils.SEC, 5 * TimeUtils.SEC, 30 * TimeUtils.SEC);
        fileReloadMgr.registerReaders(Collections.singleton(new BlackListReader()));

        fileReloadMgr.registerListener(Collections.singleton(BlackListReader.FILE_NAME), (fileNameSet, changedFileNameSet) -> {
            System.out.println("blackList changed");
            System.out.println(testDataMgr.blackList);
        });

        // 准备就绪后加载文件
        fileReloadMgr.loadAll();
        System.out.println(testDataMgr.blackList);

        // 热更新
        fileReloadMgr.reloadAll(null);

        for (int index = 0; index < 1000; index++) {
            timerSystem.tick();

            if (index % 100 == 0) {
                // 修改文件
                changeFile();
            }

            ThreadUtils.sleepQuietly(100);
        }
    }

    private static void initFile() throws IOException {
        final String relativePath = BlackListReader.FILE_NAME.getRelativePath();
        ReloadTestUtils.recreateFile(relativePath);
    }

    private static void changeFile() throws Exception {
        final List<String> blackList = List.of(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        final String relativePath = BlackListReader.FILE_NAME.getRelativePath();
        ReloadTestUtils.writeStringList(relativePath, blackList);
    }
}
