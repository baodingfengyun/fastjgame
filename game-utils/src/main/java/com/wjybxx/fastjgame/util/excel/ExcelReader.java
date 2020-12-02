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

import com.google.common.collect.Maps;
import com.monitorjbl.xlsx.StreamingReader;
import com.wjybxx.fastjgame.util.CloseableUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * {@link StreamingReader}不可以使用{@code getRow这种方法}
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/11/21
 * github - https://github.com/hl845740757
 */
class ExcelReader implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(ExcelReader.class);

    /**
     * 字母开头，只可以使用字母数字和下划线
     */
    private static final String SHEET_NAME_REGEX = "^[a-zA-Z][a-zA-Z0-9_]*$";
    private static final Pattern PATTERN = Pattern.compile(SHEET_NAME_REGEX);

    private final File file;
    private final CellValueParser parser;
    private final Predicate<String> sheetNameFilter;
    private final Workbook workbook;

    ExcelReader(File file, CellValueParser parser, Predicate<String> sheetNameFilter, final int bufferSize) {
        this.file = file;
        this.parser = Objects.requireNonNull(parser, "parser");
        this.sheetNameFilter = sheetNameFilter;
        workbook = StreamingReader.builder()
                .rowCacheSize(200)
                .bufferSize(bufferSize)
                .open(file);
    }

    Map<String, Sheet> readSheets() {
        final int numberOfSheets = workbook.getNumberOfSheets();
        final Map<String, Sheet> result = Maps.newLinkedHashMapWithExpectedSize(numberOfSheets);
        for (int sheetIndex = 0; sheetIndex < numberOfSheets; sheetIndex++) {
            final org.apache.poi.ss.usermodel.Sheet poiSheet = workbook.getSheetAt(sheetIndex);
            final String sheetName = poiSheet.getSheetName();

            if (isSheetNameSkippable(sheetName, sheetNameFilter)) {
                // 无意义的命名，跳过
                logger.info("skip sheet, sheetName is invalid, fileName {}, sheetName {}", file.getName(), sheetName);
                continue;
            }

            try {
                if (result.containsKey(sheetName)) {
                    final String msg = String.format("sheetName is duplicate, sheetName: %s", sheetName);
                    throw new IllegalStateException(msg);
                }

                final Sheet appSheet = new SheetReader(file.getName(), sheetName, sheetIndex, poiSheet, parser).read();
                if (appSheet == null) {
                    // 可能没需要读取的字段
                    logger.info("skip sheet, appSheet is null, fileName {}, sheetName {}", file.getName(), sheetName);
                    continue;
                }

                result.put(sheetName, appSheet);
            } catch (Exception e) {
                throw new ReadSheetException(file.getName(), sheetName, sheetIndex, e);
            }
        }
        return result;
    }

    private static boolean isSheetNameSkippable(String sheetName, Predicate<String> sheetNameFilter) {
        return StringUtils.isBlank(sheetName)
                || sheetName.startsWith("Sheet")
                || sheetName.startsWith("sheet")
                || !sheetNameFilter.test(sheetName)
                || !PATTERN.matcher(sheetName).matches();
    }

    @Override
    public void close() throws IOException {
        CloseableUtils.closeSafely(workbook);
    }

    static List<String> readExcelSheetNames(File file, Predicate<String> sheetNameFilter) throws IOException {
        try (final Workbook workbook = StreamingReader.builder()
                .rowCacheSize(10)
                .bufferSize(1024)
                .open(file)) {

            final int numberOfSheets = workbook.getNumberOfSheets();
            final List<String> result = new ArrayList<>();

            for (int sheetIndex = 0; sheetIndex < numberOfSheets; sheetIndex++) {
                final org.apache.poi.ss.usermodel.Sheet poiSheet = workbook.getSheetAt(sheetIndex);
                final String sheetName = poiSheet.getSheetName();
                if (isSheetNameSkippable(sheetName, sheetNameFilter)) {
                    continue;
                }
                result.add(sheetName);
            }
            return result;
        }
    }


    static Sheet readSheetUseFileSimpleName(File file, CellValueParser parser, int bufferSize) throws IOException {
        try (final Workbook workbook = StreamingReader.builder()
                .rowCacheSize(200)
                .bufferSize(bufferSize)
                .open(file)) {

            // sheetName使用excel文件的名字
            final String fileName = file.getName();
            final String sheetName = fileName.substring(0, fileName.lastIndexOf('.'));
            final int sheetIndex = 0;

            final org.apache.poi.ss.usermodel.Sheet poiSheet = workbook.getSheetAt(sheetIndex);
            final Sheet appSheet = new SheetReader(fileName, sheetName, sheetIndex, poiSheet, parser).read();
            if (appSheet == null) {
                // 内容不合法
                throw new ReadSheetException(file.getName(), sheetName, sheetIndex, "sheetContent error");
            }
            return appSheet;
        }
    }

}
