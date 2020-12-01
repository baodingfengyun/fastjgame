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

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * 参数表，表结构如下:
 * <pre>
 *            |---------------|------------|---------------|---------------|
 *  cs标记行   |     cs        |     cs     |      cs       |               |
 *            |---------------|------------|---------------|---------------|
 *  固定命名行 |   name        |    type    |    value      |     描述      |
 *            |---------------|------------|---------------|---------------|
 *   内容行    |   MAX_LEVEL   |    int32   |    100        |   最大等级     |
 *            |---------------|------------|---------------|---------------|
 *   内容行    |   NPC_NAME    |   string   |     wjybxx    |   某个NPC的名字|
 *            |---------------|------------|---------------|---------------|
 * </pre>
 * 注意：
 * 1. name的值不可以重复，我们可以通过name取到对应的value
 * 2. 任意一列标记了s，那么name/type/value三列都会读取到内存，不论是否会被使用；
 *
 * @author wjybxx
 * date - 2020/11/20
 * github - https://github.com/hl845740757
 */
public class ParamSheetContent extends CellProvider implements SheetContent {

    public static final String name = "name";
    public static final String type = "type";
    public static final String value = "value";

    public static final Set<String> PARAM_SHEET_COL_NAMES;

    static {
        PARAM_SHEET_COL_NAMES = Set.of(name, type, value);
    }

    // 表头没有意义，不存储
    /**
     * 使用key-value形式存储，而不是{@link ValueRow}形式
     */
    private final Map<String, ValueCell> name2CellMap;

    public ParamSheetContent(final Map<String, ValueCell> name2CellMap) {
        this.name2CellMap = Collections.unmodifiableMap(name2CellMap);
    }

    public Map<String, ValueCell> getName2CellMap() {
        return name2CellMap;
    }

    public Set<String> nameSet() {
        return name2CellMap.keySet();
    }

    public Collection<ValueCell> valueCells() {
        return name2CellMap.values();
    }

    @Override
    public ValueCell getCell(String name) {
        return name2CellMap.get(name);
    }

    @Override
    public int totalRowCount() {
        return name2CellMap.size() + 2;
    }

    @Override
    public int valueRowCount() {
        return name2CellMap.size();
    }
}
