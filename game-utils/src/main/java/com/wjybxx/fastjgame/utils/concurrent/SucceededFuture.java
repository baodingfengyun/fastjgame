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
import javax.annotation.Nullable;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 表示Future关联的操作早已<b>正常完成</b>。
 * 推荐使用{@link EventLoop#newSucceededFuture(Object)}代替使用该future的构造方法。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/14
 * github - https://github.com/hl845740757
 */
public class SucceededFuture<V> extends CompleteFuture<V> {

    /**
     * future关联的任务的结果。
     * 可能为null
     */
    private final V result;

    public SucceededFuture(@Nonnull EventLoop notifyExecutor, @Nullable V result) {
        super(notifyExecutor);
        this.result = result;
    }

    @Override
    public final boolean isSuccess() {
        return true;
    }

    @Override
    public final V get() throws InterruptedException, CompletionException {
        return result;
    }

    @Override
    public final V get(long timeout, @Nonnull TimeUnit unit) throws InterruptedException, CompletionException, TimeoutException {
        return result;
    }

    @Nullable
    @Override
    public final V getNow() {
        return result;
    }

    @Override
    public final V join() throws CompletionException {
        return result;
    }

    @Override
    public final Throwable cause() {
        return null;
    }

}
