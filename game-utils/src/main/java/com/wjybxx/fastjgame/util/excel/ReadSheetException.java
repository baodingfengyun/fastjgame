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

package com.wjybxx.fastjgame.util.excel;

/**
 * excel读取异常
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/11/23
 * github - https://github.com/hl845740757
 */
public class ReadSheetException extends RuntimeException {

    private final String fileName;
    private final String sheetName;
    private final int sheetIndex;

    public ReadSheetException(String fileName, String sheetName, int sheetIndex, Exception e) {
        super(String.format("fileName: %s, sheetName: %s, sheetIndex: %d", fileName, sheetName, sheetIndex), e);
        this.fileName = fileName;
        this.sheetName = sheetName;
        this.sheetIndex = sheetIndex;
    }

    public String getFileName() {
        return fileName;
    }

    public String getSheetName() {
        return sheetName;
    }

    public int getSheetIndex() {
        return sheetIndex;
    }
}
