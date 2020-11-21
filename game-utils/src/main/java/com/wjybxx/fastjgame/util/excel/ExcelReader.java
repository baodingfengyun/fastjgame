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
package com.wjybxx.fastjgame.util.excel;

import com.monitorjbl.xlsx.StreamingReader;
import com.wjybxx.util.common.CloseableUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

/**
 * Excel文件的Reader
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/11 16:18
 * github - https://github.com/hl845740757
 */
class ExcelReader extends SheetReader<Row> {

    private final String sheetName;
    private final int bufferSize;

    private Workbook workbook = null;
    private int sheetIndex;
    private Sheet sheet;

    ExcelReader(String sheetName, int bufferSize) {
        this.sheetName = sheetName;
        this.bufferSize = bufferSize;
    }

    @Override
    protected void openFile(File file) throws IOException {
        // 看源码发现open时感觉使用file更好
        workbook = StreamingReader.builder()
                .rowCacheSize(200)
                .bufferSize(bufferSize)
                .open(file);

        sheetIndex = workbook.getSheetIndex(sheetName);
        sheet = workbook.getSheetAt(sheetIndex);
    }

    @Override
    protected String sheetName() throws IOException {
        return sheet.getSheetName();
    }

    @Override
    protected int sheetIndex() {
        return sheetIndex;
    }

    @Override
    protected Iterator<Row> toRowIterator() throws IOException {
        return sheet.rowIterator();
    }

    @Override
    protected int getTotalColNum(Row row) {
        return row.getLastCellNum();
    }

    @Override
    protected String getNullableCell(Row row, int colIndex) {
        Cell cell = row.getCell(colIndex, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        return cell.getStringCellValue();
    }

    @Override
    public void close() throws Exception {
        CloseableUtils.closeSafely(workbook);
        // help gc
        sheet = null;
    }
}
