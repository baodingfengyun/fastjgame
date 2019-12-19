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

import javax.annotation.Nonnull;

/**
 * 事件处理器。
 * <p>
 * Q: 为什么该版本事件处理器默认双参数了？
 * A: 有时候我们想要把某些既有类作为事件抛出，但是我们无法修改这些类或对这些类进行包装的成本太高，因此我们期望能以组合的方式抛出事件。
 * 现实案例：接收到一个网络数据包时，该数据包解码之后，就已经包含了类型了，直接以该类型进行抛出是最简单明了的。而封装抛出事件则丢失了原始的类信息。
 * 另外，这也是没有Event接口的原因，对于既有代码或无法修改的代码来讲，该设计是很友好的。
 * <p>
 * Q: 为什么叫Context，而不是EventParam？
 * A: {@code onEvent(Context, Event)}和{@code onEvent(Event, EventParam)}有着完全不同的语义，到底哪个好，其实我也纠结了很久。
 * 后来想明白了为什么EventParam这种设计不好。
 * 什么时候会出现EventParam这样的设计？ 当<b>Event是枚举或整形值</b>的时候！此时的Event无法描述自己的特征，需要EventParam来辅助。
 * 但更好的是将Event和EventParam合并为一个自描述的的Event对象！
 * Context表述的是抛出事件时对应的上下文信息，它们是独立的两部分，不应该被合并。
 * <h3>安全问题</h3>
 * 由于无法对context对象进行约束（无法在编译期间检查），因此存在context不兼容的可能，它提高了编码难度 - 你除了要了解Event对象本身以外，还需要了解它对应的上下文类型。
 * 该设计极大的提高了灵活性，但确实也引入了不安全的因素。如果你担心风险，那么所有监听方法都使用单参数的形式。
 *
 * @param <T> 这里定义为泛型，有两个好处
 *            1. 子类可以限定context的类型
 *            2. 自己写方法的时候，不必强转。
 * @author wjybxx
 * @version 1.1
 * date - 2019/12/18
 * github - https://github.com/hl845740757
 */
public interface EventHandler<T, E> {

    /**
     * 当订阅的事件产生时该方法将被调用。
     *
     * @param context 事件关联的上下文
     * @param event   事件
     */
    void onEvent(T context, @Nonnull E event) throws Exception;

}
