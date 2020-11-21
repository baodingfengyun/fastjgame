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

import java.util.*;

/**
 * 参数表，表结构如下:
 * <pre>
 *            |---------------|------------|---------------|---------------|
 *  cs标记列   |     cs        |     cs     |      cs       |               |
 *            |---------------|------------|---------------|---------------|
 *  固定命名列 |   name        |    type    |    value      |     描述      |
 *            |---------------|------------|---------------|---------------|
 *   内容行    |   MAX_LEVEL   |    int32   |    100        |   最大等级     |
 *            |---------------|------------|---------------|---------------|
 *   内容行    |   NPC_NAME    |   string   |     wjybxx    |   某个NPC的名字|
 *            |---------------|------------|---------------|---------------|
 * </pre>
 * name的值不可以重复，我们可以通过name取到对应的value
 *
 * @author wjybxx
 * date - 2020/11/20
 * github - https://github.com/hl845740757
 */
public class ParamSheetContent implements SheetContent {

    /**
     * 内容行(使用key-value形式存储，而不是列表形式)
     */
    private final Map<String, CellValue> name2CellValueMap;

    /**
     * @param name2CellValueMap 使用{@link LinkedHashMap}保持表格顺序。
     */
    ParamSheetContent(final LinkedHashMap<String, CellValue> name2CellValueMap) {
        this.name2CellValueMap = Collections.unmodifiableMap(name2CellValueMap);
    }

    public CellValue getCellValue(String name) {
        return name2CellValueMap.get(name);
    }

    public Set<String> nameSet() {
        return name2CellValueMap.keySet();
    }

    public Collection<CellValue> cellValues() {
        return name2CellValueMap.values();
    }

    public Map<String, CellValue> getName2CellValueMap() {
        return name2CellValueMap;
    }

    @Override
    public int rowCount() {
        return name2CellValueMap.size();
    }
}
