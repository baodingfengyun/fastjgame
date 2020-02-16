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

package com.wjybxx.fastjgame.net.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用该注解注解的字段表示是一个需要序列化的属性字段。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/17
 * github - https://github.com/hl845740757
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface SerializableField {

    /**
     * 字段的具体类型，该属性用于实现精确解析。
     * <p>
     * 当一个字段是抽象的@link java.util.Map} 或 {@link java.util.Collection}时，必须指定其实现类型。
     * 并且确保其实现包含一个public的无参构造方法，注解处理器会在编译时检查。
     *
     * <h3>嵌套集合</h3>
     * 对于多重嵌套类型集合，编译期间无法提供很好的检查，因此不建议使用多重嵌套的集合。
     * 另外，{@code Map<Integer,Map<String,Integer>> }这种代码本身的可读性就较差，为其建立一些类吧。
     */
    Class<?> impl() default Object.class;

    /**
     * 如果实现层兼容性做的好的话，那么指定名字有助于提升兼容性。
     * 如果实现层未处理兼容性问题的话，那么指定名字并没有什么用。
     */
    String name() default "";

    /**
     * 一些注释信息
     */
    String comment() default "";
}
