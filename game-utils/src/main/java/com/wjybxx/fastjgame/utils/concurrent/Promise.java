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

import com.wjybxx.fastjgame.utils.annotation.UnstableApi;

import javax.annotation.Nonnull;
import java.util.concurrent.Executor;

/**
 * promise用于为关联的{@link ListenableFuture}赋值结果。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/3/6
 */
public interface Promise<V> extends ListenableFuture<V> {

    /**
     * 将future标记为成功完成。
     * <p>
     * 如果该future对应的操作早已完成(失败或成功)，将抛出一个{@link IllegalStateException}.
     */
    void setSuccess(V result);

    /**
     * 尝试将future标记为成功完成。
     *
     * @return 当且仅当成功将future标记为成功完成时返回true，如果future对应的操作已完成(成功或失败)，则返回false，并什么都不改变。
     */
    boolean trySuccess(V result);

    /**
     * 将future标记为失败完成。
     * <p>
     * 如果future对应的操作早已完成（成功或失败），则抛出一个{@link IllegalStateException}.
     */
    void setFailure(@Nonnull Throwable cause);

    /**
     * 尝试将future标记为失败完成。
     *
     * @return 当前仅当成功将future标记为失败完成时返回true，如果future对应的操作已完成（成功或失败），则返回false，并什么也不改变。
     */
    boolean tryFailure(@Nonnull Throwable cause);

    /**
     * 将future标记为失败完成。
     * 它是{@link #setFailure(Throwable)}的快捷调用，具体默认封装为什么异常，取决与实现类。
     *
     * @param msg 失败信息
     */
    void setFailure(@Nonnull String msg);

    /**
     * 将future标记为失败完成。
     * 它是{@link #tryFailure(Throwable)}的快捷调用，具体默认封装为什么异常，取决与实现类。。
     *
     * @param msg 失败信息
     * @return 当前仅当成功将future标记为失败完成时返回true，如果future对应的操作已完成（成功或失败），则返回false，并什么也不改变。
     */
    boolean tryFailure(@Nonnull String msg);

    /**
     * 将future标记为不可取消状态，它表示计算已经开始，不可以被取消。
     *
     * @return 1. 如果成功设置为不可取消 或 已经是不可取消状态 则返回true.
     * 2. 已经进入完成状态(不是被取消进入的完成状态) 返回true。
     * 否则返回false（其实也就是被取消返回false）。
     */
    boolean setUncancellable();

    /**
     * {@inheritDoc}
     * <p>
     * 如果该方法返回true，任何赋值操作都将不会造成任何影响。
     */
    @UnstableApi
    @Override
    boolean isVoid();

    // 仅用于语法支持
    @Override
    Promise<V> onComplete(@Nonnull FutureListener<? super V> listener);

    @Override
    Promise<V> onComplete(@Nonnull FutureListener<? super V> listener, @Nonnull Executor bindExecutor);

    @Override
    Promise<V> onSuccess(@Nonnull SucceededFutureListener<? super V> listener);

    @Override
    Promise<V> onSuccess(@Nonnull SucceededFutureListener<? super V> listener, @Nonnull Executor bindExecutor);

    @Override
    Promise<V> onFailure(@Nonnull FailedFutureListener<? super V> listener);

    @Override
    Promise<V> onFailure(@Nonnull FailedFutureListener<? super V> listener, @Nonnull Executor bindExecutor);
}
