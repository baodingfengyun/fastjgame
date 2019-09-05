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
 * 1.使用该注解的方法有且仅有一个参数，参数类型就是自己订阅的事件类型。如果没有参数 或 超过一个参数，那么编译时会报错。
 * 2.参数不可以带泛型，因为泛型是不具备区分度的，如果存在泛型，那么编译时会报错。
 * 3.参数类型不可以是基本类型，因为发布事件的时候会封装为Object，基本类型会被装箱，会导致问题。
 *
 * 示例类：{@link com.wjybxx.fastjgame.example.SubscriberExample}
 *
 * auto:
 * 会为拥有{@link Subscribe}方法的类生成一个代理文件，需要手动注册到EventBus上。生成的文件名字为 XXXBusRegister
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/23
 * github - https://github.com/hl845740757
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface Subscribe {

}
