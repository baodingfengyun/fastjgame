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

package com.wjybxx.fastjgame.reload;

import com.wjybxx.fastjgame.reload.mgr.FileDataMgr;
import com.wjybxx.fastjgame.reload.mgr.SheetDataMgr;
import com.wjybxx.fastjgame.reload.sheet.SkillParamTemplate;
import com.wjybxx.fastjgame.reload.sheet.SkillTemplate;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

import java.util.List;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/11/30
 * github - https://github.com/hl845740757
 */
public class ReloadTestDataMgr implements FileDataMgr, SheetDataMgr {

    public List<String> whiteList;
    public List<String> blackList;

    public SkillParamTemplate skillParamTemplate;
    public Int2ObjectMap<SkillTemplate> skillTemplateMap;

    @Override
    public ReloadTestDataMgr newInstance() {
        return new ReloadTestDataMgr();
    }
}
