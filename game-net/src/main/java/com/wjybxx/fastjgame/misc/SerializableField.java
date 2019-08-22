/*
 *    Copyright 2019 wjybxx
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.wjybxx.fastjgame.misc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用该注解注解的字段表示是一个需要序列化的属性字段。
 * 注意：对于集合类型(List,Map,Set)声明类型必须是List,Map,Set，否则对方无法反序列化。
 * 本地使用时可以使用具体类型，但是对方解析时不看具体类型。
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
	 * 该属性对应的number，不要进行修改，取值范围，正整数或0。
	 * 尽量在[0, 127]，正常情况完全够用。
	 * @return >= 0
	 */
	int number();

	/**
	 * 是否可能为负数？
	 * 如果可能为负，将会使用{@code sint32} {@code sint64}进行编码。
	 * 该属于适用于byte,short,int,long 及其包装类型。
	 * @return 如果返回true将会得到优化
	 */
	boolean mayNegative() default false;

}
