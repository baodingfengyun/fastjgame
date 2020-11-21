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
import java.util.List;

/**
 * 普通表格（纵向）
 *
 * <pre>
 *            |---------------|--------------|---------------|---------------|
 *  cs标记列   |     cs        |     cs       |      c       |       c        |
 *            |---------------|--------------|---------------|---------------|
 *  类型列     |    int32      |    int32     |    boolean    |    string     |
 *            |---------------|--------------|---------------|---------------|
 *  命名列     |   skillId     |   actionId   |   shockScreen |  animationId  |
 *            |---------------|--------------|---------------|---------------|
 *  描述列     |    技能id     |     动作id    |    是否震屏    |    动画id     |
 *            |---------------|--------------|---------------|---------------|
 *   内容行    |     10001     |    20001     |    false      |    30001      |
 *            |---------------|--------------|---------------|---------------|
 *   内容行    |     10002     |    20001     |     true      |    30002      |
 *            |---------------|--------------|---------------|---------------|
 * </pre>
 *
 * @author wjybxx
 * date - 2020/11/20
 * github - https://github.com/hl845740757
 */
public class DefaultSheetContent implements SheetContent {

    /**
     * 内容行
     */
    private final List<SheetRow> sheetRows;

    public DefaultSheetContent(List<SheetRow> sheetRows) {
        this.sheetRows = Collections.unmodifiableList(sheetRows);
    }

    public List<SheetRow> getSheetRows() {
        return sheetRows;
    }

    @Override
    public int rowCount() {
        return sheetRows.size();
    }
}
