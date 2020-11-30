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

import com.wjybxx.fastjgame.reload.excel.SheetName;
import com.wjybxx.fastjgame.reload.excel.SheetReloadListener;
import com.wjybxx.fastjgame.reload.mgr.ExcelReloadMgr;
import com.wjybxx.fastjgame.reload.mgr.FileReloadMgr;
import com.wjybxx.fastjgame.reload.mgr.ScanResult;
import com.wjybxx.fastjgame.reload.sheet.SkillParamTemplate;
import com.wjybxx.fastjgame.reload.sheet.SkillTemplate;
import com.wjybxx.fastjgame.util.ThreadUtils;
import com.wjybxx.fastjgame.util.excel.DefaultCellValueParser;
import com.wjybxx.fastjgame.util.time.TimeUtils;

import java.util.Set;

import static com.wjybxx.fastjgame.reload.FileReloadTest.PROJECT_RES_DIR;
import static com.wjybxx.fastjgame.reload.FileReloadTest.newThreadPool;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/11/30
 * github - https://github.com/hl845740757
 */
public class ExcelReloadTest {

    public static void main(String[] args) throws Exception {
        final ReloadTestDataMgr testDataMgr = new ReloadTestDataMgr();
        final ScanResult scanResult = ScanResult.valueOf(Set.of("com.wjybxx.fastjgame.reload.sheet"));
        // 初始化文件管理器相关
        final FileReloadMgr fileReloadMgr = new FileReloadMgr(PROJECT_RES_DIR, newThreadPool(), testDataMgr);
        fileReloadMgr.registerReaders(scanResult.fileReaders);
        fileReloadMgr.registerCacheBuilders(scanResult.fileCacheBuilders);
        // 初始化excel管理器相关
        final ExcelReloadMgr excelReloadMgr = new ExcelReloadMgr(PROJECT_RES_DIR, "config",
                fileReloadMgr, testDataMgr, DefaultCellValueParser::new);
        excelReloadMgr.registerReaders(scanResult.sheetReaders);
        excelReloadMgr.registerCacheBuilders(scanResult.sheetCacheBuilders);

        // 准备就绪后加载文件
        fileReloadMgr.loadAll();
        // 加载表格
        excelReloadMgr.loadAll();


        // 先输出一次基本内容
        System.out.println(testDataMgr.skillTemplateMap);
        System.out.println(testDataMgr.skillParamTemplate);

        // 注册监听器
        excelReloadMgr.registerListener(Set.of(SkillTemplate.FILL_NAME, SkillParamTemplate.FILE_NAME),
                new ReloadListener(testDataMgr));

        // 暂时懒得写一个改excel内容的实现了，手动改吧(可以文件打开的情况下，测试几波)
        for (int index = 0; index < 6; index++) {
            ThreadUtils.sleepQuietly(5 * TimeUtils.SEC);
            excelReloadMgr.reloadAll();
        }
    }

    private static class ReloadListener implements SheetReloadListener {

        final ReloadTestDataMgr testDataMgr;

        private ReloadListener(ReloadTestDataMgr testDataMgr) {
            this.testDataMgr = testDataMgr;
        }

        @Override
        public void afterReload(Set<SheetName<?>> sheetNames, Set<SheetName<?>> changedSheetNames) throws Exception {
            System.out.println("changedSheetNames: "+ changedSheetNames);
            System.out.println(testDataMgr.skillTemplateMap);
            System.out.println(testDataMgr.skillParamTemplate);
        }
    }
}
