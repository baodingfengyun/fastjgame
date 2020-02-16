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

package com.wjybxx.fastjgame.db.annotation;

import com.wjybxx.fastjgame.db.core.DBOrder;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 实体索引字段。
 * 用该注解注解的类，表示该字段是有序的，可索引的，它仅仅表示期望插件能根据该字段快速查询。
 *
 * <p>限制</p>
 * 1. 可索引的字段必须是{@link String} 或 数字类型。
 * 2. 只有当实体是最外层实体的时候，该字段有效。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/16
 * github - https://github.com/hl845740757
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface DBIndex {

    /**
     * 顺序规则
     */
    DBOrder value() default DBOrder.ASC;
}
