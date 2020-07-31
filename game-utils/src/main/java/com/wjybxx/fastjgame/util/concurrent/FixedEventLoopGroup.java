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

package com.wjybxx.fastjgame.util.concurrent;

import javax.annotation.Nonnull;
import java.util.Iterator;

/**
 * 固定{@link EventLoop}的事件循环线程组
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/3/5
 */
public interface FixedEventLoopGroup extends EventLoopGroup, Iterable<EventLoop> {

    /**
     * 给定一个键 - 分配一个{@link EventLoop}。
     * 目的：这样用户总是可以通过key指定选中某一个线程，消除不必要的同步。
     *
     * @param key 计算索引的键
     * @apiNote 必须保证同一个key分配的结果一定是相同的
     */
    @Nonnull
    EventLoop select(int key);

    /**
     * 返回{@link EventLoop}的数量。
     */
    int numChildren();

    @Nonnull
    @Override
    Iterator<EventLoop> iterator();

}
