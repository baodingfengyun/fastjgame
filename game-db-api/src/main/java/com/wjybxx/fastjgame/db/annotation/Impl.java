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

package com.wjybxx.fastjgame.db.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 该注解用于确定字段的实现类型，以实现精确解析。
 * <h3>什么时候需要？</h3>
 * 当一个字段需要序列化或持久化时，如果其声明类型是抽象的，且其运行时类型不是自定义实体时，我们需要通过该属性获取如何安全的解析对象。
 *
 * <h3>为什么需要？</h3>
 * 它是由多态产生的，当我们在序列化或持久化一个对象时，由于一个对象可以实现任意接口，如果该对象运行时类型不是自定义实体，我们就缺乏足够的信息解析它。
 * 举个栗子，当我们传输{@link java.util.LinkedList}时，如果它不是自定义实体中的一个属性，我们就不知道对方期望的类型是什么，
 * 我们并不能确定对方要一个{@link java.util.List}还是{@link java.util.Queue}，因此解析后就可能出现{@link ClassCastException}。
 * <p>
 * Q: 那传输类的全限定名行吗？
 * A: 行不通，为什么呢？
 * 举个栗子：当对方发送一个不可变集合时，你即使有全限定名，也没有办法。
 * <p>
 * Q: 那我们是否可以禁止程序里发送不可变集合或空集合呢？
 * A: 不行，基于客户端的约定并不可靠，危险，成本高。其关键是，接收方期望以什么类型接收，它对发送方并不提要求。
 *
 * <h3>一些限制</h3>
 * {@link #value()}指定的类型必须 1. 必须是具体类型 2.必须拥有public构造方法。
 * 编译时会进行检查。
 *
 * <h3>多层嵌套类型</h3>
 * 举个栗子：{@code Map<Integer, Map<String,Integer>>}
 * 对于这种类型，没有办法约束，因此没有办法保证能解析为正确的类型，因此不建议使用多重嵌套的类型。
 * 另外，这种代码本身的可读性就较差，为其建立一些类吧。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/17
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface
Impl {

    /**
     * 默认的解析类型
     */
    Class<?> value();

}
