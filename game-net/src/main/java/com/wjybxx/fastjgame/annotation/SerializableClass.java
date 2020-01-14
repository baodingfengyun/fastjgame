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

package com.wjybxx.fastjgame.annotation;

import com.wjybxx.fastjgame.enummapper.NumericalEnum;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用该注解注解的类表示是一个需要序列化的类。
 * <p>
 * 对于带有该注解的类，注解处理器需要提供以下保证：
 * 1. 如果是枚举，必须实现{@link NumericalEnum}，并提供forNumber方法 - 也就是按照protoBuf的枚举格式来。
 * 2. 如果是实现了{@link NumericalEnum}的类，必须提供提供forNumber方法。
 * 3. 如果是普通类，必须提供无参构造方法，可以是private。
 * <p>
 * 如果对象是一个普通的javabean，则会在编译时生成对应的{@link com.wjybxx.fastjgame.misc.BeanSerializer}类，可以代替反射。
 * javaBean:
 * 1. 无参构造方法非private
 * 2. 要序列化的字段存在对应的getter 和 setter方法。
 * 如果对象不是javaBean，需要实现为不可变对象或其它，则会使用反射进行编解码。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/17
 * github - https://github.com/hl845740757
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SerializableClass {

}
