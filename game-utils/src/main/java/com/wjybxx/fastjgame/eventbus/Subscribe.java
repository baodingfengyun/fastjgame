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

package com.wjybxx.fastjgame.eventbus;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 表示订阅一个事件。
 * 注意：
 * 1.使用该注解的方法有且仅有一个参数，该参数的类型就是自己订阅的事件类型。如果没有参数 或 超过一个参数，那么编译时会报错。
 * 2.如果期望订阅多个事件，请使用{@link #subEvents()}声明关注的其它事件。
 * 3.参数不可以带泛型，因为泛型是不具备区分度的，如果存在泛型，那么编译时会报错。
 * 4.参数类型不可以是基本类型，因为发布事件的时候会封装为Object，基本类型会被装箱，会导致问题。
 * 5.方法不能是private - 至少是包级访问权限。
 * <p>
 * 示例类：{@link com.wjybxx.fastjgame.example.SubscriberExample}
 * <p>
 * 注解处理器会为拥有{@link Subscribe}方法的类生成一个代理文件，需要手动调用生成的register方法注册到{@link EventHandlerRegistry}。
 * 生成的代理类为 XXXBusRegister 。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/23
 * github - https://github.com/hl845740757
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface Subscribe {

    /**
     * 是否只订阅子事件类型
     * (不为方法参数生成事件注册方法，只为{@link #subEvents()}中的事件生成注册方法)
     */
    boolean onlySubEvents() default false;

    /**
     * 声明需要订阅子事件。
     * 注意：这里声明的类型必须是方法参数的子类型，否则编译错误。
     */
    Class[] subEvents() default {};

}
