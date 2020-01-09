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

package com.wjybxx.fastjgame.async;

import com.wjybxx.fastjgame.concurrent.FutureResult;
import com.wjybxx.fastjgame.concurrent.GenericFutureResultListener;

import javax.annotation.Nonnull;
import java.util.concurrent.ExecutionException;

/**
 * 可刷新缓冲区的异步(远程)服务句柄
 *
 * @param <T> the type of method spec
 * @author wjybxx
 * @version 1.0
 * date - 2020/1/9
 * github - https://github.com/hl845740757
 */
public interface FlushableServiceHandle<T extends MethodSpec<?>, FR extends FutureResult<?>> extends ServiceHandle<T, FR> {

    /**
     * 异步执行一个方法，同时刷新命令队列，并不监听结果。
     *
     * @param methodSpec 待执行方法说明(描述信息)
     */
    void executeAndFlush(@Nonnull T methodSpec);

    /**
     * 异步执行一个方法，同时刷新命令队列，并在完成时通知指定的监听器。
     *
     * @param methodSpec 待执行方法说明(描述信息)
     * @param listener   监听器(回调执行线程根据实现而定)
     */
    void callAndFlush(@Nonnull T methodSpec, GenericFutureResultListener<FR> listener);

    /**
     * {@inheritDoc}
     *
     * @apiNote 同步调用是很紧急的，因此该方法实现类必须刷新缓冲区，以尽快执行同步调用。
     */
    <V> V syncCall(@Nonnull T methodSpec) throws ExecutionException;
}
