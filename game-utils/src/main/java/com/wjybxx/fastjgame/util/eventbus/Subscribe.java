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

package com.wjybxx.fastjgame.util.eventbus;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 表示订阅一个事件。
 * 注意：
 * 1. 使用该注解的方法必须有且仅有一个参数。
 * 2. <b>如果参数是{@link GenericEvent}的实现类，则其泛型参数为订阅的事件类型，否则该参数表示订阅的事件类型。</b>
 * 3.< b>如果期望监听某一类事件，请将{@link GenericEvent}的泛型参数声明为通配符'?'</b>
 * 4. 方法参数不可以是基本类型，因为发布事件的时候会封装为Object，基本类型会被装箱，会导致问题。
 * 5. 如果期望订阅多个事件，请使用{@link #subEvents()}声明关注的其它事件。
 * 6. 方法不能是private - 至少是包级访问权限。
 * <p>
 * 注解处理器会为拥有{@link Subscribe}方法的类生成一个代理文件，需要手动调用生成的register方法注册到{@link EventHandlerRegistry}。
 * 生成的代理类为 XXXBusRegister。
 * 注意：在使用注解之后，需要先编译一次，生成对应的文件，这样你可以引用到对应的类，后续不必多编译一次。
 * <p>
 * Q: 如果想使用多个EventBus，如果避免订阅方法注册到不该注册的地方？
 * A: 有两种选择，一：使用带过滤器的EventBus，这样可以筛选掉不期望的事件类型。
 * 二：使用多个类进行订阅（内部类也可以），内部类A订阅X类型的事件，内部类B订阅Y类型的事件，内部类C订阅Z类型的事件。
 * 推荐使用第二种方式，尤其是使用内部类的方式，不过需要内部类和它的方法的访问权限至少为包级。
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
     * 是否只订阅子事件类型。
     * 如果为true，则表示不为方法参数生成事件注册方法，只为{@link #subEvents()}中的事件生成注册方法。
     * 注意：一般只在你使用了{@link #subEvents()}时才需要设置该属性。
     */
    boolean onlySubEvents() default false;

    /**
     * 声明需要订阅子事件。
     * 注意：这里声明的类型必须是方法参数的子类型，否则编译错误。
     */
    Class[] subEvents() default {};

    /**
     * 用于特定{@link EventBus}的特定数据。
     * <p>
     * Q: 它的作用？
     * A: 告诉特定的事件处理器以实现一些特定的横切面处理，比如：控制频率。
     * <p>
     * 由于是注解处理器，因此不能定义对象，所以建议使用Json，尽量保持可读。
     */
    String customData() default "";

    /**
     * 注释文档
     */
    String comment() default "";
}
