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

package com.wjybxx.fastjgame.net.binary;

import com.wjybxx.fastjgame.utils.dsl.IndexableValue;
import com.wjybxx.fastjgame.utils.dsl.IndexableEnum;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用该注解注解的类表示是一个需要序列化的类。
 * <h3>注解处理器</h3>
 * 对于带有该注解的类，注解处理器需要提供以下保证：
 * 1. 如果是枚举，必须实现{@link IndexableEnum}，并提供非private的{@code forNumber(int)}方法 - 也就是按照protoBuf的枚举格式来。
 * 2. 如果是实现了{@link IndexableEnum}的类，也必须提供提供非private的{@code forNumber(int)}方法。
 * 3. 如果是实现了{@link IndexableValue}的类，必须提供非private的{@code forIndex(Object)}方法。
 * 4. 如果是普通类，必须提供<b>无参构造方法</b>，可以是private，且要序列化的字段必须提供非private的getter方法，setter方法根据自己需求决定是否提供。
 *
 * <h3>扩展</h3>
 * Q: 是否可以不使用注解，也能序列化？
 * A: 所有带有该注解的类，注解处理器都会生成对应的编解码器。如果不使用注解，但是为指定类手动实现了{@link PojoCodecImpl}，那么对应的类就可以序列化。
 *
 * <h3>性能</h3>
 * 生成的类中，能使用普通方法调用的，就会使用普通方法调用(构造方法、取值、设值方法)，如果字段是final的，或没有相应的取值、设值方法，就会使用反射。
 * 应用代码最好不要考虑这里的细节问题，并不建议对象所有要序列化的字段都是可修改的，该是final的还是final，eg:不可变对象/值对象。
 *
 * <h3>一些建议</h3>
 * 1. 一般而言，建议使用注解{@link SerializableClass}，并遵循相关规范，由注解处理器生成的类负责解析，而不是实现{@link PojoCodecImpl}。
 * 仅当某些类使用大量的反射调用进行编解码导致性能瓶颈时，才应该考虑实现{@link PojoCodecImpl}负责编解码相关的类，那么不需要该注解。
 * 2. 并不建议都实现为javabean格式。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/17
 * github - https://github.com/hl845740757
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface SerializableClass {

}
