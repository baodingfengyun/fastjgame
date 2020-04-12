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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据库实体注解。
 * 使用该注解标记的类，表示需要持久化。
 * 注意，该注解仅仅标注指定类需要持久化，但不限制具体存储在哪里，以什么方式存储，只要满足应用需求即可。
 *
 * <h3>“DB” == “数据库” ？</h3>
 * 这里的数据库是一个抽象的概念，只是代表一个存取数据的地方，不一定真的是数据库，也可以是文件系统，甚至是内存。
 * 也不限定存储格式，<b>只要能满足应用存取数据需求</b>，SQL或者NOSQL都是可以的，取决于需求和最终使用的插件。
 * 因此，这里并没有引入'table'和'document'概念，而是使用{@link #name()}来表达。
 *
 * <h3>实现要求</h3>
 * 1. 必须正确存取，{@link DBEntity}标注的类必须是可以持久化，并正确读写的，{@link DBField}标记的字段是要持久化的字段。
 * 2. 必须支持序列化，{@link DBEntity}标注的类必须是可以序列化的，{@link DBField}标记的字段必须要序列化，至于其它字段是否序列化，取决于用户需求。
 * 3. 必须支持<b>继承</b>和<b>组合</b>，不能限制应用程序构建数据模型的方式。
 *
 * <h3>兼容问题</h3>
 * 不要求实现层提供运行时的兼容性，但必须提供工具方便数据迁移。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/16
 * github - https://github.com/hl845740757
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DBEntity {

    /**
     * 实体的唯一名字，用于序列化及存取数据时确定实体。
     * 用户必须保证唯一。
     * 尽量不要修改，否则可能需要进行数据迁移。
     * <p>
     * Q: 为什么没有默认值？
     * A: 持久化层对正确性要求要高于一般的序列化，对待存储的数据必须慎重。
     *
     * <p>
     * Q: 为什么不是数字？
     * A: 1. 数字不具备表达力，不可读。
     * 2. 应用程序中，入库的实体对象可能非常多，不论怎么管理id分配，对于使用者来说都是反人类的，即使有一个类定义了所有的常量。
     * 3. 如果实现层支持某种debug模式的话，可以方便调试；
     */
    String name();

    /**
     * 一些注释信息
     */
    String comment() default "";
}
