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

package com.wjybxx.fastjgame.db.core;

import com.wjybxx.fastjgame.net.binary.Impl;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据库实体字段
 * 注意：{@link java.util.Map}和{@link java.util.Collection}多态问题解决方案请查看{@link Impl}注解。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/16
 * github - https://github.com/hl845740757
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface DBField {

    /**
     * 字段持久化时的名字。
     * 持久化结果中尽量包含字段名字信息，尽量直观可读。
     */
    String name();

    /**
     * 一些注释信息
     */
    String comment() default "";
}
