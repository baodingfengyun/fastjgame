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

import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Excel和Csv读表超类，模板实现。
 * 推荐使用{@link ExcelCsvUtils}中的静态方法读取表格。
 *
 * @param <T> the type of row
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/11 16:16
 * github - https://github.com/hl845740757
 * @apiNote 如果你自己有自己的实现，请注意在{@link #close()}中关闭流等资源。
 */
abstract class SheetReader<T> implements AutoCloseable {

    /**
     * 读取表格的模板方法
     *
     * @param file         要读取的表格文件
     * @param nameRowIndex 属性名索引(0开始) 前面的行表示注释行或者标记行
     * @return {@link Sheet}
     * @throws Exception error
     */
    final Sheet readCfg(File file, int nameRowIndex) throws Exception {
        // 打开文件
        openFile(file);

        final String sheetName = sheetName();
        validateSheetName(sheetName);
        final int sheetIndex = sheetIndex();

        final Iterator<T> rowItr = toRowIterator();

        final List<SheetRow> sheetRows = new ArrayList<>();
        ColNameRow colNameRow = null;

        // 缓存前面的行
        final List<T> cacheRows = new ArrayList<>(nameRowIndex);

        for (int rowIndex = 0; ; rowIndex++) {
            if (!rowItr.hasNext()) {
                break;
            }

            T row = rowItr.next();
            // 前面的行
            if (rowIndex < nameRowIndex) {
                cacheRows.add(row);
                continue;
            }
            if (rowIndex == nameRowIndex) {
                colNameRow = readColNameRow(sheetName, rowIndex, row);
                // 把前面行也读取进来
                for (int cacheRowIndex = 0; cacheRowIndex < rowIndex; cacheRowIndex++) {
                    SheetRow sheetRow = readContentRow(sheetName, colNameRow, cacheRowIndex, cacheRows.get(cacheRowIndex));
                    sheetRows.add(sheetRow);
                }
            }
            assert null != colNameRow;
            SheetRow sheetRow = readContentRow(sheetName, colNameRow, rowIndex, row);
            sheetRows.add(sheetRow);
        }

        if (null == colNameRow) {
            throw new IllegalArgumentException("sheet " + sheetName + " missing colNameRow");
        }

        return new Sheet(file.getName(), sheetName, sheetIndex, colNameRow, sheetRows);
    }

    private void validateSheetName(String sheetName) {
        if (StringUtils.isBlank(sheetName)) {
            throw new IllegalArgumentException("sheetName is blank");
        }
    }

    /**
     * 打开文件
     */
    protected abstract void openFile(File file) throws IOException;

    /**
     * 获取页签的名字
     */
    protected abstract String sheetName() throws IOException;

    /**
     * 获取页签的索引
     */
    protected abstract int sheetIndex();

    /**
     * 将文件转换为可迭代的行
     */
    protected abstract Iterator<T> toRowIterator() throws IOException;

    /**
     * 获取指定行有多少列
     */
    protected abstract int getTotalColNum(T row);

    /**
     * 获取指定行指定列索引的元素
     *
     * @param row      原始行类型
     * @param colIndex [0,getTotalColNum(row))
     * @return nullable
     */
    protected abstract String getNullableCell(T row, int colIndex);

    /**
     * 读取属性名行
     *
     * @param sheetName 页签名
     * @param rowIndex  行索引
     * @param row       行内容
     * @return 命名行
     */
    private ColNameRow readColNameRow(String sheetName, int rowIndex, T row) {
        // 使用LinkedHashMap以保持读入顺序
        int totalColNum = getTotalColNum(row);
        Object2IntMap<String> colName2Index = new Object2IntLinkedOpenHashMap<>(totalColNum + 1);
        for (int colIndex = 0; colIndex < totalColNum; colIndex++) {
            String originalColName = getNullableCell(row, colIndex);
            // 属性名称行，空白属性跳过
            if (null == originalColName) {
                continue;
            }
            // 去掉空白填充
            String realColName = originalColName.trim();
            if (realColName.length() == 0) {
                continue;
            }
            // 属性名不可以有重复
            if (colName2Index.containsKey(realColName)) {
                throw new IllegalArgumentException("sheet " + sheetName
                        + " propertyNameRow has duplicate column " + realColName);
            }
            colName2Index.put(realColName, colIndex);
        }
        return new ColNameRow(rowIndex, colName2Index);
    }

    /**
     * 读取内容行
     *
     * @param sheetName  页签名
     * @param colNameRow 属性列信息
     * @param rowIndex   行索引
     * @param row        行内容
     * @return 内容行
     */
    private SheetRow readContentRow(String sheetName, ColNameRow colNameRow, int rowIndex, T row) {
        boolean isBlackLine = true;
        // 使用LinkedHashMap以保持读入顺序
        Map<String, String> colName2Value = new LinkedHashMap<>();
        // 读取所有属性
        for (Object2IntMap.Entry<String> entry : colNameRow.getColName2Index().object2IntEntrySet()) {
            String colName = entry.getKey();
            int colIndex = entry.getIntValue();
            String nullableCell = getNullableCell(row, colIndex);
            String value = null == nullableCell ? "" : nullableCell;

            if (!StringUtils.isBlank(value)) {
                isBlackLine = false;
            }
            colName2Value.put(colName, value);
        }

        if (isBlackLine) {
            final String msg = String.format("sheetName: %s, rowIndex: %d is blank line", sheetName, rowIndex);
            throw new IllegalArgumentException(msg);
        }

        return new SheetRow(rowIndex, colName2Value);
    }

}
