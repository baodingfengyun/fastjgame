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

import javax.annotation.Nonnull;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/11/30
 * github - https://github.com/hl845740757
 */
public class SkillParamTemplate {

    public static final SheetName<SkillParamTemplate> FILE_NAME = SheetName.valueOf("SkillParam");

    public final float DEFAULT_HIT_RATE;
    public final float DEFAULT_CRIT_RATE;

    private SkillParamTemplate(Sheet sheet) {
        DEFAULT_HIT_RATE = sheet.getValueCell("DEFAULT_HIT_RATE").readAsFloat();
        DEFAULT_CRIT_RATE = sheet.getValueCell("DEFAULT_CRIT_RATE").readAsFloat();
    }

    @Override
    public String toString() {
        return "SkillParamTemplate{" +
                "DEFAULT_HIT_RATE=" + DEFAULT_HIT_RATE +
                ", DEFAULT_CRIT_RATE=" + DEFAULT_CRIT_RATE +
                '}';
    }

    private static class SkillParamReader implements SheetReader<SkillParamTemplate> {

        @Nonnull
        @Override
        public SheetName<SkillParamTemplate> sheetName() {
            return FILE_NAME;
        }

        @Nonnull
        @Override
        public SkillParamTemplate read(Sheet sheet) {
            return new SkillParamTemplate(sheet);
        }

        @Override
        public void assignTo(SkillParamTemplate sheetData, SheetDataMgr sheetDataMgr) {
            ((ReloadTestDataMgr) sheetDataMgr).skillParamTemplate = sheetData;
        }

        @Override
        public void validateOther(SheetDataMgr sheetDataMgr) {

        }
    }
}
