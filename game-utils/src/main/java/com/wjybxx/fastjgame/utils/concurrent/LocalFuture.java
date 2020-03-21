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

package com.wjybxx.fastjgame.utils.concurrent;

import javax.annotation.Nonnull;
import java.util.concurrent.Executor;

/**
 * 只允许特定线程进行监听的future。
 * 它基于特定的假设，进行了一些激进的优化。
 *
 * <h3>假设</h3>
 * 1. 我们假设用户只在特定线程添加监听器。
 * 2. 我们假设用户添加的监听器绝大多数是回调到当前线程的。
 * 3. 我们假设future进入完成状态时，总是有监听器待通知。
 *
 * <h3>激进优化</h3>
 * 1. 基于假设1和假设2，我们总是在特定线程下才通知监听器，这样可以消除<b>添加</b>和<b>删除</b>监听器过程中产生的竞争。<br>
 * 2. 基于假设3，当future进入完成状态时，只是简单的提交一个通知任务，而不需要任何变量判断 <b>是否存在监听器</b>，<b>是否正在通知监听器</b>。
 *
 * <p>
 * 上面的假设，其实是很常见的情况，而且应该占多数。
 * 只允许特定线程进行监听，相对于允许多线程监听，会更轻量级，可以有效的减少开销。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/3/8
 * github - https://github.com/hl845740757
 */
public interface LocalFuture<V> extends ListenableFuture<V> {

    /**
     * 默认的监听器执行环境，同时只有该线程下才可以添加监听器
     */
    @Override
    EventLoop defaultExecutor();

    // 用于语法支持
    @Override
    LocalFuture<V> addListener(@Nonnull FutureListener<? super V> listener);

    @Override
    LocalFuture<V> addListener(@Nonnull FutureListener<? super V> listener, @Nonnull Executor bindExecutor);

}
