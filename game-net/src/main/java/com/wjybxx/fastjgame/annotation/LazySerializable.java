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
 * 表示rpc方法中的某个参数可以延迟到网络层序列化为字节数组！
 * <p>
 * Q: 我是在什么情境下想到这个的呢？<b>
 * A: 逻辑服务器向网关服务器发送消息，而这个消息是需要转发给玩家的。我并不想这个消息在网关服进行不必要的编解码操作。最开始想到的方案有两种：<b>
 * 1. 其实最简单的办法就是应用层直接序列化为字节数组，然后发送单向消息或rpc发给网关服。但是它致命的一点是应用层要进行序列化。这个在消息量大的时候会很影响应用层性能。<b>
 * 2. 另一种就是新增协议，但是新增协议的最大问题是无法很好的和单向消息和rpc调用配合，当期望监听调用结果的时候，也不好搞，要加的东西很多。<b>
 * <p>
 * 思考了2-3个小时，没着急下手，最后想到这个注解，如果目标方法的参数是{@link byte[]}，且带有该注解，那么生成的代理将该类型替换为{@link Object}，并在写入
 * 网络之前，将所有带该注解的参数序列化为字节数组。该解决方案更加通用。
 * <p>
 * 对代码生成产生的影响：
 * 1. 不可以使用{@link java.util.Collections#singletonList(Object)}，因为无法修改数据。
 * 2. 对{@link byte[]}类型且带有{@link LazySerializable} 的参数替换为{@link Object}类型。
 * 3. 为提高编码速度，必须进行索引，避免不必要的遍历。
 *
 * <p>
 * 注意：
 * 1. 该注解只能用在{@link byte[]}上，否则编译报错
 * 2. 代理方法参数类型为Object
 *
 * <p>
 * PS: 应当有个配对的，提前解码的注解，后面有空再加。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/10/10
 * github - https://github.com/hl845740757
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.PARAMETER)
public @interface LazySerializable {

}
