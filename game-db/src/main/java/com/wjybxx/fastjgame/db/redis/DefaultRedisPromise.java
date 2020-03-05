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

package com.wjybxx.fastjgame.db.redis;


import com.wjybxx.fastjgame.utils.ConcurrentUtils;
import com.wjybxx.fastjgame.utils.concurrent.DefaultPromise;
import com.wjybxx.fastjgame.utils.concurrent.EventLoop;
import com.wjybxx.fastjgame.utils.concurrent.FutureListener;

import javax.annotation.Nonnull;
import java.util.concurrent.Executor;

/**
 * redis异步操作结果的默认实现
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/12/12
 * github - https://github.com/hl845740757
 */
public class DefaultRedisPromise<V> extends DefaultPromise<V> implements RedisPromise<V> {

    /**
     * 工作线程 - 检查死锁的线程
     */
    private final RedisEventLoop workerEventLoop;

    DefaultRedisPromise(RedisEventLoop workerEventLoop, EventLoop appEventLoop) {
        super(appEventLoop);
        this.workerEventLoop = workerEventLoop;
    }

    @Override
    protected void checkDeadlock() {
        ConcurrentUtils.checkDeadLock(workerEventLoop);
    }

    // ------------------------------------------------ 支持流式语法 ------------------------------------

    @Override
    public RedisPromise<V> await() throws InterruptedException {
        super.await();
        return this;
    }

    @Override
    public RedisPromise<V> awaitUninterruptibly() {
        super.awaitUninterruptibly();
        return this;
    }

    @Override
    public RedisPromise<V> addListener(@Nonnull FutureListener<? super V> listener) {
        super.addListener(listener);
        return this;
    }

    @Override
    public RedisPromise<V> addListener(@Nonnull FutureListener<? super V> listener, @Nonnull Executor bindExecutor) {
        super.addListener(listener, bindExecutor);
        return this;
    }

    @Override
    public RedisPromise<V> removeListener(@Nonnull FutureListener<? super V> listener) {
        super.removeListener(listener);
        return this;
    }
}
