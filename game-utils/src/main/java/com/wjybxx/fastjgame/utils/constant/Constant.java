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

package com.wjybxx.fastjgame.utils.constant;

/**
 * 常量抽象，主要参考Netty实现，但又有所不同。
 * <p>
 * Q: 常量的含义？
 * A: 常量类似于枚举，仅仅使用==判断相等性，一般由{@link ConstantPool}创建。与其说像{@link Enum}，不如说更像{@link ThreadLocal}
 * <p>
 * Q: 使用常量时需要注意的地方？
 * A: 1. 一般由{@link ConstantPool}创建。
 * 2. 其使用方式与{@link ThreadLocal}非常相似，优先定义静态属性，只有有足够理由的时候才定义非静态属性。
 *
 * <p>
 * Q: 为什么没有id属性？
 * A: 与枚举不同，常量可以在任意地方定义，因此常量的创建顺序受到类初始化顺序的影响，也就导致了id属性的不稳定。
 * 虽然在某些情况下，可以保证其创建顺序是确定的，但是尽量延迟做决定是否支持它。
 * 拿Netty的{@code AttributeKey}来讲，应用程序是可以在任意地方创建的，也就导致了{@code AttributeKey}的id是不稳定的。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/7/31
 */
public interface Constant<T extends Constant<T>> extends Comparable<T> {

    /**
     * 返回常量的名字。
     * 注意：即使名字相同，也不代表是同一个同一个常量，只有同一个引用时才一定相等。
     */
    String name();

}
