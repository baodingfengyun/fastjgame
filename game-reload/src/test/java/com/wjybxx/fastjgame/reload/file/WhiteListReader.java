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

package com.wjybxx.fastjgame.reload.file;

import com.wjybxx.fastjgame.reload.ReloadTestDataMgr;
import com.wjybxx.fastjgame.reload.mgr.FileDataMgr;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/11/30
 * github - https://github.com/hl845740757
 */
public class WhiteListReader implements FileReader<List<String>> {

    public static final FileName<List<String>> FILE_NAME = FileName.valueOf("txt/WhiteList.txt");

    @Nonnull
    @Override
    public FileName<List<String>> fileName() {
        return FILE_NAME;
    }

    @Nonnull
    @Override
    public List<String> read(File file) throws Exception {
        try (BufferedReader reader = new BufferedReader(new java.io.FileReader(file))) {
            final List<String> whiteList = new ArrayList<>(2);
            String account;
            while ((account = reader.readLine()) != null) {
                whiteList.add(account);
            }
            return whiteList;
        }
    }

    @Override
    public void assignTo(List<String> fileData, FileDataMgr fileDataMgr) {
        ((ReloadTestDataMgr) fileDataMgr).whiteList = fileData;
    }

    @Override
    public void validateOther(FileDataMgr fileDataMgr) {

    }
}
