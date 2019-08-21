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
package com.wjybxx.fastjgame.concurrent;


import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.TimeUnit;

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

    /**
     * Creates a new instance.
     *
     * @param executor the {@link EventLoop} associated with this future
     */
    public SucceededFuture(@Nonnull EventLoop executor, @Nullable V result) {
        super(executor);
        this.result = result;
    }

    @Override
    public Throwable cause() {
        return null;
    }

    @Override
    public boolean isSuccess() {
        return true;
    }

    @Override
    public V get() {
        return result;
    }

    @Override
    public V get(long timeout, @Nonnull TimeUnit unit) {
        return result;
    }

    @Nullable
    @Override
    public V getNow() {
        return result;
    }
}
