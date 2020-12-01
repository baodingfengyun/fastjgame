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

package com.wjybxx.fastjgame.reload.sheet;

import com.wjybxx.fastjgame.reload.ReloadTestDataMgr;
import com.wjybxx.fastjgame.reload.excel.SheetName;
import com.wjybxx.fastjgame.reload.excel.SheetReader;
import com.wjybxx.fastjgame.reload.mgr.SheetDataMgr;
import com.wjybxx.fastjgame.util.excel.Sheet;
import com.wjybxx.fastjgame.util.excel.ValueRow;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import javax.annotation.Nonnull;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/11/30
 * github - https://github.com/hl845740757
 */
public class SkillTemplate {

    public static final SheetName<Int2ObjectMap<SkillTemplate>> FILL_NAME = SheetName.valueOf("Skill");

    public final int skillId;
    public final int actionId;

    public SkillTemplate(int skillId, int actionId) {
        this.skillId = skillId;
        this.actionId = actionId;
    }

    @Override
    public String toString() {
        return "SkillTemplate{" +
                "skillId=" + skillId +
                ", actionId=" + actionId +
                '}';
    }

    private static class SkillReader implements SheetReader<Int2ObjectMap<SkillTemplate>> {

        @Nonnull
        @Override
        public SheetName<Int2ObjectMap<SkillTemplate>> sheetName() {
            return FILL_NAME;
        }

        @Nonnull
        @Override
        public Int2ObjectMap<SkillTemplate> read(Sheet sheet) {
            final Int2ObjectMap<SkillTemplate> result = new Int2ObjectOpenHashMap<>(sheet.valueRowCount());
            for (ValueRow valueRow : sheet.getValueRows()) {
                final int skillId = valueRow.getCell("skillId").readAsInt();
                final int actionId = valueRow.getCell("actionId").readAsInt();
                result.put(skillId, new SkillTemplate(skillId, actionId));
            }
            return result;
        }

        @Override
        public void assignTo(Int2ObjectMap<SkillTemplate> sheetData, SheetDataMgr sheetDataMgr) {
            ((ReloadTestDataMgr) sheetDataMgr).skillTemplateMap = sheetData;
        }

        @Override
        public void validateOther(SheetDataMgr sheetDataMgr) {

        }
    }
}
