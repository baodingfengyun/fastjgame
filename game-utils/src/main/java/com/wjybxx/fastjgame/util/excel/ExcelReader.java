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

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.monitorjbl.xlsx.StreamingReader;
import com.wjybxx.fastjgame.util.CloseableUtils;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * {@link StreamingReader}不可以使用{@code getRow这种方法}
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/11/21
 * github - https://github.com/hl845740757
 */
class ExcelReader implements AutoCloseable {

    private final File file;
    private final CellValueParser parser;
    private final Workbook workbook;

    ExcelReader(File file, CellValueParser parser, final int bufferSize) {
        this.file = file;
        this.parser = Objects.requireNonNull(parser, "parser");
        workbook = StreamingReader.builder()
                .rowCacheSize(200)
                .bufferSize(bufferSize)
                .open(file);
    }

    List<Sheet> readSheets() throws IOException {
        final int numberOfSheets = workbook.getNumberOfSheets();
        final List<Sheet> result = new ArrayList<>(numberOfSheets);
        final Set<String> sheetNameSet = Sets.newHashSetWithExpectedSize(numberOfSheets);
        for (int sheetIndex = 0; sheetIndex < numberOfSheets; sheetIndex++) {
            final org.apache.poi.ss.usermodel.Sheet poiSheet = workbook.getSheetAt(sheetIndex);

            final String sheetName = poiSheet.getSheetName();
            if (StringUtils.isBlank(sheetName)
                    || sheetName.startsWith("Sheet")
                    || sheetName.startsWith("sheet")) {
                // 无意义的命名，跳过
                continue;
            }

            if (!sheetNameSet.add(sheetName)) {
                final String msg = String.format("sheetName is duplicate, file: %s. sheetName: %s",
                        file.getName(), sheetName);
                throw new IllegalArgumentException(msg);
            }

            final Sheet appSheet = readSheet(poiSheet, sheetIndex);
            if (appSheet == null) {
                // 可能没需要读取的字段
                continue;
            }

            result.add(appSheet);
        }
        return result;
    }

    private Sheet readSheet(org.apache.poi.ss.usermodel.Sheet poiSheet, int sheetIndex) throws IOException {
        final int totalRowCount = getTotalRowCount(poiSheet);
        if (totalRowCount == 0) {
            // 空表
            return null;
        }

        // 第一行为cs标记和可选参数信息
        final Row firstRow = poiSheet.rowIterator().next();
        if (!containsServerCellColumn(firstRow)) {
            // 不包含服务器字段
            return null;
        }

        final SheetContent sheetContent;
        if (isNormalSheet(poiSheet)) {
            sheetContent = readNormalSheet(poiSheet);
        } else if (isParamSheet(poiSheet)) {
            sheetContent = readParamSheet(poiSheet);
        } else {
            throw new IllegalArgumentException("unrecognized sheet, sheetName: " + poiSheet.getSheetName());
        }

        return new Sheet(file.getName(), poiSheet.getSheetName(), sheetIndex, sheetContent);
    }

    private boolean containsServerCellColumn(Row firstRow) {
        final int totalColCount = getTotalColCount(firstRow);
        for (int colIndex = 0; colIndex < totalColCount; colIndex++) {
            final String firstRowCellValue = getCellValueNonNull(firstRow, colIndex);
            if (isServerCell(firstRowCellValue)) {
                return true;
            }
        }
        return false;
    }

    // 转义
    private static int getTotalRowCount(org.apache.poi.ss.usermodel.Sheet sheet) {
        return sheet.getLastRowNum();
    }

    // 转义
    private static int getTotalColCount(Row row) {
        return row.getLastCellNum();
    }

    private static String getCellValueNonNull(Row row, int colIndex) {
        // 未填充时，返回空字符串
        final Cell cell = row.getCell(colIndex);
        if (cell == null) {
            return "";
        }

        if (cell.getCellType() == CellType.NUMERIC
                || cell.getCellType() == CellType.STRING
                || cell.getCellType() == CellType.BOOLEAN
                || cell.getCellType() == CellType.BLANK) {
            return Objects.requireNonNullElse(cell.getStringCellValue(), "");
        }

        throw new IllegalStateException("Unsupported cellType: " + cell.getCellType());
    }

    private static boolean isServerCell(String cellValue) {
        if (StringUtils.isBlank(cellValue)) {
            return false;
        }

        // "s"检测包含了"sc"检测
        if (cellValue.startsWith("s") || cellValue.startsWith("cs")) {
            return true;
        }

        if (!cellValue.startsWith("c")) {
            throw new IllegalArgumentException("the cell in the firstRow must start with c or s, value: " + cellValue);
        }
        return false;
    }

    private boolean isNormalSheet(org.apache.poi.ss.usermodel.Sheet poiSheet) {
        if (getTotalRowCount(poiSheet) < 4) {
            // 至少4行
            return false;
        }
        // 判断需要读取的列的第二行是否都是类型信息
        final Iterator<Row> rowItr = poiSheet.rowIterator();
        final Row firstRow = rowItr.next();
        final Row secondRow = rowItr.next();

        final int totalColCount = getTotalColCount(firstRow);
        for (int colIndex = 0; colIndex < totalColCount; colIndex++) {
            final String firstRowCellValue = getCellValueNonNull(firstRow, colIndex);
            if (!isServerCell(firstRowCellValue)) {
                continue;
            }

            final String secondRowCellValue = getCellValueNonNull(secondRow, colIndex);
            if (!parser.supportedTypes().contains(secondRowCellValue)) {
                return false;
            }
        }
        return true;
    }

    private boolean isParamSheet(org.apache.poi.ss.usermodel.Sheet poiSheet) {
        if (getTotalRowCount(poiSheet) < 2) {
            return false;
        }
        final Iterator<Row> rowItr = poiSheet.rowIterator();
        skip(rowItr, 1);
        // 判断第二列是否包含name/type/value三列
        final Row secondRow = rowItr.next();
        final Set<String> expectedNameSet = new HashSet<>(ParamSheetContent.PARAM_SHEET_COL_NAMES);
        final int totalColCount = getTotalColCount(secondRow);
        for (int colIndex = 0; colIndex < totalColCount; colIndex++) {
            final String cellValue = getCellValueNonNull(secondRow, colIndex);
            expectedNameSet.remove(cellValue);
        }
        return expectedNameSet.isEmpty();
    }

    private SheetContent readNormalSheet(org.apache.poi.ss.usermodel.Sheet poiSheet) throws IOException {
        return new DefaultSheetContentReader(poiSheet, parser).read();
    }

    private SheetContent readParamSheet(org.apache.poi.ss.usermodel.Sheet poiSheet) throws IOException {
        return new ParamSheetContentReader(poiSheet, parser).read();
    }

    @Override
    public void close() throws Exception {
        CloseableUtils.closeSafely(workbook);
    }

    interface SheetContentReader {

        SheetContent read() throws IOException;

    }

    private static void skip(Iterator<?> iterator, int n) {
        while (iterator.hasNext() && n-- > 0) {
            iterator.next();
        }
    }

    /**
     * 定义类避免大规模传参
     */
    static class DefaultSheetContentReader implements SheetContentReader {

        private final org.apache.poi.ss.usermodel.Sheet poiSheet;
        private final CellValueParser parser;

        DefaultSheetContentReader(org.apache.poi.ss.usermodel.Sheet poiSheet, CellValueParser parser) {
            this.poiSheet = poiSheet;
            this.parser = parser;
        }

        @Override
        public SheetContent read() {
            // 索引名称行
            final Object2IntMap<String> name2ColIndexMap = readNameRow();
            final Iterator<Row> rowItr = poiSheet.rowIterator();
            // 表头行
            final List<HeaderRow> headerRows = new ArrayList<>(4);
            headerRows.add(readHeaderRow(name2ColIndexMap, rowItr.next()));
            headerRows.add(readHeaderRow(name2ColIndexMap, rowItr.next()));
            headerRows.add(readHeaderRow(name2ColIndexMap, rowItr.next()));
            headerRows.add(readHeaderRow(name2ColIndexMap, rowItr.next()));
            // 内容行
            final List<ValueRow> valueRows = new ArrayList<>(getTotalRowCount(poiSheet) - 4);
            while (rowItr.hasNext()) {
                valueRows.add(readContentRow(name2ColIndexMap, rowItr.next()));
            }
            return new DefaultSheetContent(headerRows, valueRows);
        }

        private Object2IntMap<String> readNameRow() {
            final Iterator<Row> rowItr = poiSheet.rowIterator();
            final Row firstRow = rowItr.next();
            skip(rowItr, 1);
            final Row nameRow = rowItr.next();

            final int totalColCount = getTotalColCount(nameRow);
            // 使用LinkedHashMap以保持读入顺序
            final Object2IntMap<String> name2ColIndexMap = new Object2IntLinkedOpenHashMap<>(totalColCount);
            for (int colIndex = 0; colIndex < totalColCount; colIndex++) {
                final String firstRowCellValue = getCellValueNonNull(firstRow, colIndex);
                if (!isServerCell(firstRowCellValue)) {
                    continue;
                }

                final String colName = getCellValueNonNull(nameRow, colIndex);
                if (StringUtils.isBlank(colName)) {
                    // 属性名不可以空白
                    final String msg = String.format("the name cannot be blank, sheetName: %s， colIndex: %d",
                            poiSheet.getSheetName(), colIndex);
                    throw new IllegalStateException(msg);
                }

                if (name2ColIndexMap.containsKey(colName)) {
                    // 属性名不可以有重复
                    final String msg = String.format("the name is duplicate, sheetName: %s， name: %s",
                            poiSheet.getSheetName(), colName);
                    throw new IllegalStateException(msg);
                }

                name2ColIndexMap.put(colName, colIndex);
            }
            return name2ColIndexMap;
        }

        private HeaderRow readHeaderRow(Object2IntMap<String> sheetColNameRow, Row headerRow) {
            // 使用LinkedHashMap以保持读入顺序
            final LinkedHashMap<String, HeaderCell> colName2Value = new LinkedHashMap<>();
            for (Object2IntMap.Entry<String> entry : sheetColNameRow.object2IntEntrySet()) {
                final String colName = entry.getKey();
                final int colIndex = entry.getIntValue();

                final String value = getCellValueNonNull(headerRow, colIndex);
                final HeaderCell headerCell = new HeaderCell(colName, value);
                colName2Value.put(colName, headerCell);
            }
            return new HeaderRow(getRowIndex(headerRow), colName2Value);
        }

        /**
         * 读取内容行
         *
         * @param sheetColNameRow 属性列信息
         * @param valueRow        内容行
         * @return 内容行
         */
        private ValueRow readContentRow(Object2IntMap<String> sheetColNameRow, Row valueRow) {
            final Iterator<Row> rowItr = poiSheet.rowIterator();
            skip(rowItr, 1);
            final Row secondRow = rowItr.next();

            // 空白行检测
            boolean isBlackLine = true;
            // 使用LinkedHashMap以保持读入顺序
            final LinkedHashMap<String, ValueCell> colName2Value = new LinkedHashMap<>();
            // 读取所有属性
            for (Object2IntMap.Entry<String> entry : sheetColNameRow.object2IntEntrySet()) {
                final String colName = entry.getKey();
                final int colIndex = entry.getIntValue();

                final String type = getCellValueNonNull(secondRow, colIndex);
                final String value = getCellValueNonNull(valueRow, colIndex);

                final DefaultValueCell valueCell = new DefaultValueCell(parser, colName, type, value);
                colName2Value.put(colName, valueCell);

                if (isBlackLine && !StringUtils.isBlank(value)) {
                    isBlackLine = false;
                }
            }

            if (isBlackLine) {
                // 内容行不可以空白
                final String msg = String.format("sheetName: %s, rowIndex: %d is blank line",
                        poiSheet.getSheetName(), getRowIndex(valueRow));
                throw new IllegalStateException(msg);
            }

            return new ValueRow(getRowIndex(valueRow), colName2Value);
        }

        // 转义
        private static int getRowIndex(Row row) {
            return row.getRowNum();
        }
    }

    static class ParamSheetContentReader implements SheetContentReader {

        private final org.apache.poi.ss.usermodel.Sheet poiSheet;
        private final CellValueParser parser;

        ParamSheetContentReader(org.apache.poi.ss.usermodel.Sheet poiSheet, CellValueParser parser) {
            this.poiSheet = poiSheet;
            this.parser = parser;
        }

        @Override
        public SheetContent read() {
            final Object2IntMap<String> name2ColIndexMap = readNameRow();
            final int nameColIndex = name2ColIndexMap.getInt(ParamSheetContent.name);
            final int typeColIndex = name2ColIndexMap.getInt(ParamSheetContent.type);
            final int valueColIndex = name2ColIndexMap.getInt(ParamSheetContent.value);

            final int totalRowCount = getTotalRowCount(poiSheet);
            final LinkedHashMap<String, ValueCell> name2ValueMap = Maps.newLinkedHashMapWithExpectedSize(totalRowCount - 2);

            final Iterator<Row> rowItr = poiSheet.rowIterator();
            skip(rowItr, 2);

            while (rowItr.hasNext()) {
                final Row valueRow = rowItr.next();
                final String name = getCellValueNonNull(valueRow, nameColIndex);
                final String type = getCellValueNonNull(valueRow, typeColIndex);
                final String value = getCellValueNonNull(valueRow, valueColIndex);

                if (name2ValueMap.containsKey(name)) {
                    // 变量命不可以重复
                    final String msg = String.format("the name is duplicate, sheetName: %s， name: %s",
                            poiSheet.getSheetName(), name);
                    throw new IllegalStateException(msg);
                }

                final DefaultValueCell valueCell = new DefaultValueCell(parser, name, type, value);
                name2ValueMap.put(name, valueCell);
            }

            return new ParamSheetContent(name2ValueMap);
        }

        private Object2IntMap<String> readNameRow() {
            final Iterator<Row> rowItr = poiSheet.rowIterator();
            skip(rowItr, 1);
            final Row nameRow = rowItr.next();
            final int totalColCount = getTotalColCount(nameRow);
            // 使用LinkedHashMap以保持读入顺序
            final Object2IntMap<String> name2ColIndexMap = new Object2IntLinkedOpenHashMap<>(3);
            for (int colIndex = 0; colIndex < totalColCount; colIndex++) {
                // 注意：只有有一列标记了s，则三列都读取到内存中，不论是否使用，因此这里不检验第一列
                final String colName = getCellValueNonNull(nameRow, colIndex);
                if (!ParamSheetContent.PARAM_SHEET_COL_NAMES.contains(colName)) {
                    continue;
                }
                name2ColIndexMap.put(colName, colIndex);
                if (name2ColIndexMap.size() == ParamSheetContent.PARAM_SHEET_COL_NAMES.size()) {
                    break;
                }
            }
            return name2ColIndexMap;
        }
    }

}
