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
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;

import java.io.IOException;
import java.util.*;

/**
 * 由于{@link com.monitorjbl.xlsx.StreamingReader}只能顺序读，会导致代码复杂度上升，避免逻辑混杂，因此定义类封装。
 *
 * @author wjybxx
 * date - 2020/11/23
 * github - https://github.com/hl845740757
 */
class SheetReader {

    private final String fileName;
    private final int sheetIndex;
    private final CellValueParser parser;

    private final int totalRowCount;
    private final String sheetName;
    private final Iterator<Row> rowItr;

    SheetReader(String fileName, int sheetIndex, org.apache.poi.ss.usermodel.Sheet poiSheet, CellValueParser parser) {
        this.fileName = fileName;
        this.sheetIndex = sheetIndex;
        this.parser = parser;

        // 避免使用错误，这里把依赖项全部提取
        totalRowCount = poiSheet.getLastRowNum();
        sheetName = poiSheet.getSheetName();
        rowItr = poiSheet.rowIterator();
    }

    Sheet read() throws IOException {
        if (totalRowCount < 2) {
            // (至少需要两行)
            return null;
        }

        // 第一行为cs标记和可选参数信息
        final Row firstRow = rowItr.next();
        if (!containsServerCellColumn(firstRow)) {
            // 不包含服务器字段
            return null;
        }

        final Row secondRow = rowItr.next();
        final SheetContent sheetContent;
        if (isParamSheet(firstRow, secondRow)) {
            sheetContent = readParamSheet(firstRow, secondRow, rowItr);
        } else {
            ensureNormalSheet(firstRow, secondRow);
            sheetContent = readNormalSheet(firstRow, secondRow, rowItr);
        }
        return new Sheet(fileName, sheetName, sheetIndex, sheetContent);
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

    private boolean isParamSheet(Row firstRow, Row secondRow) {
        // 判断第二列是否包含name/type/value三列
        final Set<String> expectedNameSet = new HashSet<>(ParamSheetContent.PARAM_SHEET_COL_NAMES);
        final int totalColCount = getTotalColCount(secondRow);
        for (int colIndex = 0; colIndex < totalColCount; colIndex++) {
            final String cellValue = getCellValueNonNull(secondRow, colIndex);
            expectedNameSet.remove(cellValue);
        }
        return expectedNameSet.isEmpty();
    }

    private void ensureNormalSheet(Row firstRow, Row secondRow) {
        if (totalRowCount < 4) {
            throw new IllegalArgumentException("unrecognized sheet, sheetName: " + sheetName);
        }

        final int totalColCount = getTotalColCount(firstRow);
        for (int colIndex = 0; colIndex < totalColCount; colIndex++) {
            final String firstRowCellValue = getCellValueNonNull(firstRow, colIndex);
            if (!isServerCell(firstRowCellValue)) {
                continue;
            }

            final String secondRowCellValue = getCellValueNonNull(secondRow, colIndex);
            if (!parser.supportedTypes().contains(secondRowCellValue)) {
                final String msg = String.format("unrecognized type, sheetName: %s, valueType: %s", sheetName, secondRowCellValue);
                throw new IllegalArgumentException(msg);
            }
        }
    }

    private SheetContent readNormalSheet(Row firstRow, Row secondRow, Iterator<Row> rowItr) throws IOException {
        return new DefaultSheetContentReader(firstRow, secondRow, rowItr, sheetName, totalRowCount, parser).read();
    }

    private SheetContent readParamSheet(Row firstRow, Row secondRow, Iterator<Row> rowItr) throws IOException {
        return new ParamSheetContentReader(firstRow, secondRow, rowItr, sheetName, totalRowCount, parser).read();
    }

    interface SheetContentReader {

        SheetContent read() throws IOException;

    }

    /**
     * 定义类避免大规模传参
     */
    static class DefaultSheetContentReader implements SheetContentReader {

        final Row firstRow;
        final Row secondRow;
        final Iterator<Row> rowItr;

        final String sheetName;
        final int totalRowCount;
        final CellValueParser parser;

        final Row thirdRow;
        final Row fourthRow;

        DefaultSheetContentReader(Row firstRow, Row secondRow, Iterator<Row> rowItr,
                                  String sheetName, int totalRowCount, CellValueParser parser) {
            this.firstRow = firstRow;
            this.secondRow = secondRow;
            this.rowItr = rowItr;
            this.sheetName = sheetName;
            this.totalRowCount = totalRowCount;
            this.parser = parser;

            thirdRow = rowItr.next();
            fourthRow = rowItr.next();
        }

        @Override
        public SheetContent read() {
            // 索引名称行
            final Object2IntMap<String> name2ColIndexMap = readNameRow();
            // 表头行
            final List<HeaderRow> headerRows = new ArrayList<>(4);
            headerRows.add(readHeaderRow(name2ColIndexMap, firstRow));
            headerRows.add(readHeaderRow(name2ColIndexMap, secondRow));
            headerRows.add(readHeaderRow(name2ColIndexMap, thirdRow));
            headerRows.add(readHeaderRow(name2ColIndexMap, fourthRow));
            // 内容行
            final List<ValueRow> valueRows = new ArrayList<>(totalRowCount - 4);
            while (rowItr.hasNext()) {
                valueRows.add(readContentRow(name2ColIndexMap, rowItr.next()));
            }
            return new DefaultSheetContent(headerRows, valueRows);
        }

        private Object2IntMap<String> readNameRow() {
            final Row nameRow = thirdRow;
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
                            sheetName, colIndex);
                    throw new IllegalStateException(msg);
                }

                if (name2ColIndexMap.containsKey(colName)) {
                    // 属性名不可以有重复
                    final String msg = String.format("the name is duplicate, sheetName: %s， name: %s",
                            sheetName, colName);
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
                        sheetName, getRowIndex(valueRow));
                throw new IllegalStateException(msg);
            }

            return new ValueRow(getRowIndex(valueRow), colName2Value);
        }

    }

    // 转义
    private static int getRowIndex(Row row) {
        return row.getRowNum();
    }

    static class ParamSheetContentReader implements SheetContentReader {

        final Row firstRow;
        final Row secondRow;
        final Iterator<Row> rowItr;

        final String sheetName;
        final int totalRowCount;
        final CellValueParser parser;

        ParamSheetContentReader(Row firstRow, Row secondRow, Iterator<Row> rowItr, String sheetName, int totalRowCount, CellValueParser parser) {
            this.firstRow = firstRow;
            this.secondRow = secondRow;
            this.rowItr = rowItr;
            this.sheetName = sheetName;
            this.totalRowCount = totalRowCount;
            this.parser = parser;
        }

        @Override
        public SheetContent read() {
            final Object2IntMap<String> name2ColIndexMap = readNameRow();
            final int nameColIndex = name2ColIndexMap.getInt(ParamSheetContent.name);
            final int typeColIndex = name2ColIndexMap.getInt(ParamSheetContent.type);
            final int valueColIndex = name2ColIndexMap.getInt(ParamSheetContent.value);

            final LinkedHashMap<String, ValueCell> name2ValueMap = Maps.newLinkedHashMapWithExpectedSize(totalRowCount - 2);
            while (rowItr.hasNext()) {
                final Row valueRow = rowItr.next();
                final String name = getCellValueNonNull(valueRow, nameColIndex);
                final String type = getCellValueNonNull(valueRow, typeColIndex);
                final String value = getCellValueNonNull(valueRow, valueColIndex);

                if (StringUtils.isBlank(name)) {
                    final String msg = String.format("the name cannot be blank, sheetName: %s, rowIndex: %d", sheetName, getRowIndex(valueRow));
                    throw new IllegalStateException(msg);
                }

                if (name2ValueMap.containsKey(name)) {
                    // 变量命不可以重复
                    final String msg = String.format("the name is duplicate, sheetName: %s， name: %s", sheetName, name);
                    throw new IllegalStateException(msg);
                }

                final DefaultValueCell valueCell = new DefaultValueCell(parser, name, type, value);
                name2ValueMap.put(name, valueCell);
            }

            return new ParamSheetContent(name2ValueMap);
        }

        private Object2IntMap<String> readNameRow() {
            final Row nameRow = secondRow;
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
