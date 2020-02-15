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

import com.wjybxx.fastjgame.net.misc.BeanSerializer;
import com.wjybxx.fastjgame.utils.enummapper.NumericalEnum;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用该注解注解的类表示是一个需要序列化的类。
 * <p>
 * 对于带有该注解的类，注解处理器需要提供以下保证：
 * 1. 如果是枚举，必须实现{@link NumericalEnum}，并提供非private的{@code forNumber(int)}方法 - 也就是按照protoBuf的枚举格式来。
 * 2. 如果是实现了{@link NumericalEnum}的类，也必须提供提供非private的{@code forNumber(int)}方法。
 * 3. 如果是普通类，必须提供无参构造方法，可以是private。
 * <p>
 * 如果对象是一个普通的javabean，则会在编译时生成对应的{@link BeanSerializer}类，可以代替反射(编解码性能提升巨大)。
 * 虽然如此，但不强制所有对象都要安装javaBean的格式，有额外需要也相当正常(eg:不可变对象)，对于非javabean类，则会使用反射进行编解码。
 * <p>
 * javaBean:
 * 1. 无参构造方法非private
 * 2. 要序列化的字段存在对应的getter 和 setter方法。
 * <p>
 * 注意：
 * 1. 一般而言，建议使用注解{@link SerializableClass}，并遵循相关规范，由注解处理器生成的类负责解析，而不是实现{@link BeanSerializer}。
 * 2. 仅当反射编解码的类存在性能瓶颈时，才应该考虑实现{@link BeanSerializer}负责编解码相关的类，那么不需要该注解。
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
