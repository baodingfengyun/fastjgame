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
package com.wjybxx.fastjgame.apt.db;

import com.wjybxx.fastjgame.apt.core.MyAbstractProcessor;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import java.util.Set;

/**
 * DB实体注解处理器（和序列化的规则其实差不多，只是最终生成的代码不一样）
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/17
 * github - https://github.com/hl845740757
 */
public class DBEntityProcessor extends MyAbstractProcessor {

    public static final String DB_ENTITY_CANONICAL_NAME = "com.wjybxx.fastjgame.db.annotation.DBEntity";
    public static final String DB_FIELD_CANONICAL_NAME = "com.wjybxx.fastjgame.db.annotation.DBField";

    private static final String DB_ID_CANONICAL_NAME = "com.wjybxx.fastjgame.db.annotation.DBId";
    private static final String DB_INDEX_CANONICAL_NAME = "com.wjybxx.fastjgame.db.annotation.DBIndex";

    public static final String IMPL_CANONICAL_NAME = "com.wjybxx.fastjgame.db.annotation.Impl";

    private TypeElement dbEntityTypeElement;
    private DeclaredType dbFieldDeclaredType;
    private DeclaredType dbIdDeclaredType;
    private DeclaredType dbIndexDeclaredType;
    private DeclaredType impDeclaredType;

    public DBEntityProcessor() {
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(DB_ENTITY_CANONICAL_NAME);
    }

    @Override
    protected void ensureInited() {

    }

    @Override
    protected boolean doProcess(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        return false;
    }
}
