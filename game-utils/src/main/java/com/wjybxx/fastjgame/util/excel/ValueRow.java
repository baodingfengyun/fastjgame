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

import java.util.Collections;
import java.util.Map;

/**
 * Excel的内容行
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/11 16:11
 * github - https://github.com/hl845740757
 */
public class ValueRow {

    /**
     * 所属的行号，0开始（实际上4开始，因为0-3为表头行）
     */
    private final int rowIndex;
    /**
     * 本行内容
     * cellName -> cellValue 可读性更好
     */
    private final Map<String, ValueCell> name2CellValueMap;

    /**
     * create instance
     *
     * @param rowIndex          行索引
     * @param name2CellValueMap 属性名到属性值的映射
     */
    public ValueRow(int rowIndex, Map<String, ValueCell> name2CellValueMap) {
        this.rowIndex = rowIndex;
        this.name2CellValueMap = Collections.unmodifiableMap(name2CellValueMap);
    }

    public int getRowIndex() {
        return rowIndex;
    }

    public Map<String, ValueCell> getName2CellValueMap() {
        return name2CellValueMap;
    }

    /**
     * @param name 列名/参数名
     * @return 该列对应的值
     */
    public ValueCell getCellValue(String name) {
        return name2CellValueMap.get(name);
    }

    @Override
    public String toString() {
        return "TableRow{" +
                "rowIndex=" + rowIndex +
                ", colName2Value=" + name2CellValueMap +
                '}';
    }
}
