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
import com.wjybxx.fastjgame.util.ThreadUtils;
import com.wjybxx.fastjgame.util.time.TimeProviders;
import com.wjybxx.fastjgame.util.timer.DefaultTimerSystem;
import com.wjybxx.fastjgame.util.timer.TimerSystem;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/11/30
 * github - https://github.com/hl845740757
 */
public class FileReloadTest {

    public static void main(String[] args) throws Exception {
        initFile();

        final ReloadTestDataMgr testDataMgr = new ReloadTestDataMgr();
        final TimerSystem timerSystem = new DefaultTimerSystem(TimeProviders.realtimeProvider());
        final FileReloadMgr fileReloadMgr = new FileReloadMgr(ReloadTestUtils.PROJECT_RES_DIR, testDataMgr, timerSystem, ReloadTestUtils.newThreadPool());
        fileReloadMgr.registerReaders(Collections.singleton(new WhiteListReader()));

        fileReloadMgr.registerListener(Collections.singleton(WhiteListReader.FILE_NAME), (fileNameSet, changedFileNameSet) -> {
            System.out.println("whiteList changed");
            System.out.println(testDataMgr.whiteList);
        });

        // 准备就绪后加载文件
        fileReloadMgr.loadAll();
        System.out.println(testDataMgr.whiteList);

        // 修改文件
        changeFile();

        // 热更新
        final AtomicBoolean stopped = new AtomicBoolean(false);
        fileReloadMgr.reloadAll((set, throwable) -> {
            stopped.set(true);
        });

        while (!stopped.get()) {
            timerSystem.tick();
            ThreadUtils.sleepQuietly(50);
        }
    }

    private static void initFile() throws IOException {
        final String relativePath = WhiteListReader.FILE_NAME.getRelativePath();
        ReloadTestUtils.recreateFile(relativePath);
    }

    private static void changeFile() throws Exception {
        final List<String> whiteList = List.of("wjybxx", "qswy");
        final String relativePath = WhiteListReader.FILE_NAME.getRelativePath();
        ReloadTestUtils.writeStringList(relativePath, whiteList);
    }

}
