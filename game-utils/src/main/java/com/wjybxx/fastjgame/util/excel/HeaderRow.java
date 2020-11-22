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

import java.util.Collections;
import java.util.Map;

/**
 * Excel的表头行
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/11/21
 * github - https://github.com/hl845740757
 */
public class HeaderRow {

    /**
     * 所属的行号，0开始
     */
    private final int rowIndex;
    /**
     * name -> cell 可读性更好
     */
    private final Map<String, HeaderCell> name2CellMap;

    public HeaderRow(int rowIndex, Map<String, HeaderCell> name2CellMap) {
        this.rowIndex = rowIndex;
        this.name2CellMap = Collections.unmodifiableMap(name2CellMap);
    }

    public int getRowIndex() {
        return rowIndex;
    }

    public Map<String, HeaderCell> getName2CellMap() {
        return name2CellMap;
    }
}
