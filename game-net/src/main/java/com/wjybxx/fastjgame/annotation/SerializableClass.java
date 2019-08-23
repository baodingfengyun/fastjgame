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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用该注解注解的类表示是一个需要序列化的类。
 *
 * 注意：
 * 1. 必须提供无参构造方法，可以是private。
 * 2. 如果是枚举类型，必须实现{@link com.wjybxx.fastjgame.enummapper.NumberEnum}接口和
 * 提供静态方法{@code forNumber(int)}。 -- 也就是按照protoBuf的枚举格式来。
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
