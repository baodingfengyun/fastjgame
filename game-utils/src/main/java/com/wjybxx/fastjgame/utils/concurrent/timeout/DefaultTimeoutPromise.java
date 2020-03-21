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

package com.wjybxx.fastjgame.utils.concurrent.timeout;

import com.wjybxx.fastjgame.utils.concurrent.DefaultBlockingPromise;
import com.wjybxx.fastjgame.utils.concurrent.EventLoop;
import com.wjybxx.fastjgame.utils.concurrent.FutureListener;

import javax.annotation.Nonnull;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeoutException;

/**
 * TimeoutPromise的默认实现。
 * 默认实现以{@link TimeoutException}表示超时。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/1/6
 * github - https://github.com/hl845740757
 */
public class DefaultTimeoutPromise<V> extends DefaultBlockingPromise<V> implements TimeoutFuture<V>, TimeoutPromise<V> {

    public DefaultTimeoutPromise(@Nonnull EventLoop defaultExecutor) {
        super(defaultExecutor);
    }

    public DefaultTimeoutPromise(@Nonnull EventLoop defaultExecutor, boolean isWorkingExecutor) {
        super(defaultExecutor, isWorkingExecutor);
    }

    @Override
    public boolean isTimeout() {
        return cause() instanceof TimeoutException;
    }

    // ------------------------------------------------ 流式语法支持 ----------------------------------------------------

    @Override
    public DefaultTimeoutPromise<V> addListener(@Nonnull FutureListener<? super V> listener) {
        super.addListener(listener);
        return this;
    }

    @Override
    public DefaultTimeoutPromise<V> addListener(@Nonnull FutureListener<? super V> listener, @Nonnull Executor bindExecutor) {
        super.addListener(listener, bindExecutor);
        return this;
    }

}
