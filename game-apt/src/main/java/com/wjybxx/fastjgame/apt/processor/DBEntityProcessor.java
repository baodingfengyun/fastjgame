package com.wjybxx.fastjgame.apt.processor;

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

    static final String DB_ENTITY_CANONICAL_NAME = "com.wjybxx.fastjgame.db.annotation.DBEntity";
    static final String DB_FIELD_CANONICAL_NAME = "com.wjybxx.fastjgame.db.annotation.DBField";
    private static final String DB_ID_CANONICAL_NAME = "com.wjybxx.fastjgame.db.annotation.DBId";
    private static final String DB_INDEX_CANONICAL_NAME = "com.wjybxx.fastjgame.db.annotation.DBIndex";
    static final String IMPL_CANONICAL_NAME = "com.wjybxx.fastjgame.db.annotation.Impl";

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
