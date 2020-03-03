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

import com.wjybxx.fastjgame.utils.annotation.UnstableApi;
import com.wjybxx.fastjgame.utils.concurrent.DefaultPromise;
import com.wjybxx.fastjgame.utils.concurrent.EventLoop;
import com.wjybxx.fastjgame.utils.concurrent.FutureListener;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.TimeUnit;
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
public class DefaultTimeoutPromise<V> extends DefaultPromise<V> implements TimeoutPromise<V> {

    /**
     * 最终时间。
     */
    private final long deadline;

    public DefaultTimeoutPromise(@Nonnull EventLoop defaultExecutor, long timeout, TimeUnit timeUnit) {
        super(defaultExecutor);
        deadline = System.currentTimeMillis() + timeUnit.toMillis(timeout);
    }

    protected DefaultTimeoutPromise(long timeout, TimeUnit timeUnit) {
        super();
        deadline = System.currentTimeMillis() + timeUnit.toMillis(timeout);
    }

    @Override
    public boolean isTimeout() {
        return isDefaultTimeout(cause());
    }

    static boolean isDefaultTimeout(Throwable cause) {
        return cause instanceof TimeoutException;
    }

    // ----------------------------------------------- 获取超时时间信息 ---------------------------------------------------
    @Override
    public long getExpireMillis() {
        return deadline;
    }

    @Override
    public long getExpire(TimeUnit timeUnit) {
        return timeUnit.convert(deadline, TimeUnit.MILLISECONDS);
    }

    // ---------------------------------------------- 非阻塞式获取结果超时检测 ------------------------------------------------

    private void checkTimeout() {
        // 如果时间到了，还没有结果，那么需要标记为超时
        if (!isDone() && System.currentTimeMillis() >= deadline) {
            onTimeout();
        }
    }

    protected void onTimeout() {
        tryFailure(new TimeoutException());
    }

    @Nullable
    @Override
    public final V getNow() {
        checkTimeout();
        return super.getNow();
    }

    @UnstableApi
    @Nullable
    @Override
    public TimeoutFutureResult<V> getAsResult() {
        checkTimeout();
        return getAsResultImp(DefaultTimeoutFutureResult::new);
    }

    @Nullable
    @Override
    public Throwable cause() {
        checkTimeout();
        return super.cause();
    }

    // -------------------------------------------------- 阻塞式等待超时检测 --------------------------------

    @Override
    public TimeoutPromise<V> await() throws InterruptedException {
        // 有限的等待
        await(remainTimeMillis(), TimeUnit.MILLISECONDS);
        assert isDone();
        return this;
    }

    @Override
    public TimeoutPromise<V> awaitUninterruptibly() {
        // 有限的等待
        awaitUninterruptibly(remainTimeMillis(), TimeUnit.MILLISECONDS);
        assert isDone();
        return this;
    }

    @Override
    public final boolean await(long timeout, @Nonnull TimeUnit unit) throws InterruptedException {
        final long waitMillis = unit.toMillis(timeout);
        final long remainMillis = remainTimeMillis();
        //  如果等待的时间超过剩余时间，那么必须有结果
        if (waitMillis >= remainMillis) {
            if (super.await(remainMillis, TimeUnit.MILLISECONDS)) {
                return true;
            }
            onTimeout();
            return true;
        } else {
            return super.await(waitMillis, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public final boolean awaitUninterruptibly(long timeout, @Nonnull TimeUnit unit) {
        final long waitMillis = unit.toMillis(timeout);
        final long remainMillis = remainTimeMillis();
        //  如果等待的时间超过剩余时间，那么必须有结果
        if (waitMillis >= remainMillis) {
            if (super.awaitUninterruptibly(remainMillis, TimeUnit.MILLISECONDS)) {
                return true;
            }
            onTimeout();
            return true;
        } else {
            return super.awaitUninterruptibly(waitMillis, TimeUnit.MILLISECONDS);
        }
    }

    private long remainTimeMillis() {
        return deadline - System.currentTimeMillis();
    }

    // ---------------------------------------------- 流式语法 ------------------------------------------------

    @Override
    public TimeoutPromise<V> addListener(@Nonnull FutureListener<? super V> listener) {
        super.addListener(listener);
        return this;
    }

    @Override
    public TimeoutPromise<V> addListener(@Nonnull FutureListener<? super V> listener, @Nonnull EventLoop bindExecutor) {
        super.addListener(listener, bindExecutor);
        return this;
    }

    @Override
    public TimeoutPromise<V> removeListener(@Nonnull FutureListener<? super V> listener) {
        super.removeListener(listener);
        return this;
    }
}
