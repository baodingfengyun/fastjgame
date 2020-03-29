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

package com.wjybxx.fastjgame.utils.concurrent;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.util.concurrent.Executor;

/**
 * 非阻塞的可监听的future。
 * 它不提供任何的阻塞式接口，只提供异步监听和非阻塞获取结果的api。
 *
 * <h3>{@link ListenableFuture}优先</h3>
 * 如果不是必须需要阻塞式的API，应当优先选择{@link ListenableFuture}。
 *
 * <h3>监听器执行时序</h3>
 * 执行环境相同的监听器，执行顺序和添加顺序相同，也就是说：<br>
 * 1. 对于未指定{@link Executor}的监听器，执行顺序和添加顺序一致，它们会在{@link #defaultExecutor()}中有序执行。<br>
 * 2. 对于指定了相同{@link Executor}的监听器，如果{@link Executor}是单线程的，那么该{@link Executor}关联的监听会按照添加顺序执行。<br>
 * 主要目的是为了降低开发难度，避免用户考虑太多的时序问题！
 *
 * <p>
 * Q: 为什么要保证执行环境相同的监听器，先添加的先执行，后添加的后执行？ <br>
 * A: 执行环境相同的监听器，如果后添加的回调先执行，将非常危险!!!<br>
 * 举个极端的例子，以下同一个方法体的两句代码,为了更清晰的说明时序问题，没有使用流式语法:
 * <pre> {@code
 *      future.addListener(this::doSomethingA, appEventLoop);
 *      future.addListener(this::doSomethingB, appEventLoop);
 * }</pre>
 * 如果不提供时序保证，那么 {@code doSomethingB} 方法可能先执行，如果回调B执行的时候，总是认为回调A已经执行了的话，将非常危险。
 *
 * <h3>实现要求</h3>
 * 1. 必须满足监听器的执行时序要求。
 * 2. 要么是线程安全的，可以多线程使用的；要么能检测到冲突并防止数据被破坏。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/3/6
 */
@ThreadSafe
public interface ListenableFuture<V> extends IFuture<V> {

    // ------------------------------------- 监听 --------------------------------------

    /**
     * 监听器的默认执行环境，如果在添加监听器时，未指定{@link Executor}，那么一定会在该{@link EventLoop}下执行。
     * 它声明为{@link EventLoop}，表示强调它是单线程的，也就是说所有未指定{@link Executor}的监听器会按照添加顺序执行。
     * <p>
     * 将其限制为单线程的，可能会导致一定的性能损失，但是可以降低编程难度。
     * 而且如果该{@link EventLoop}就是我们的业务逻辑线程，且只有业务逻辑线程添加回调的话，那么将没有性能损失，甚至可能获得性能提升，这种情况也很常见。
     */
    EventLoop defaultExecutor();

    /**
     * 添加一个监听器。Listener将会在Future计算完成时{@link #isDone() true}被通知，且最终运行在{@link #defaultExecutor()}下。
     * 如果当前Future已经计算完成，那么将立即被通知（但不一定立即执行）。
     *
     * @param listener 要添加的监听器。
     * @return this
     */
    ListenableFuture<V> addListener(@Nonnull FutureListener<? super V> listener);

    /**
     * 添加一个监听器。Listener将会在Future计算完成时{@link #isDone() true}被通知，并最终运行在指定的{@link Executor}下。
     * 如果当前Future已经计算完成，那么将立即被通知（但不一定立即执行）。
     *
     * @param listener     要添加的监听器
     * @param bindExecutor 监听器的最终执行线程
     * @return this
     * @see #addListener(FutureListener)
     */
    ListenableFuture<V> addListener(@Nonnull FutureListener<? super V> listener, @Nonnull Executor bindExecutor);

}
