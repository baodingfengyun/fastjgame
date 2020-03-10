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

package com.wjybxx.fastjgame.utils.concurrent.adapter;

import com.wjybxx.fastjgame.utils.concurrent.BlockingPromise;
import com.wjybxx.fastjgame.utils.concurrent.DelegateBlockingFuture;
import com.wjybxx.fastjgame.utils.concurrent.EventLoop;
import com.wjybxx.fastjgame.utils.concurrent.ListenableFuture;

import java.util.concurrent.CompletableFuture;

/**
 * JDK{@link CompletableFuture}的适配器。
 * <p>
 * 其实在最开始构建并发组件的时候，我就想过是选择{@link ListenableFuture}还是JDK的{@link CompletableFuture}，
 * 扫一遍{@link CompletableFuture}，它的api实在是太多了，理解和使用成本都太高，不适合暴露给逻辑程序员使用，而对其进行封装的成本更高，
 * 且游戏内一般并不需要特别多的功能，所以最终选择了{@link ListenableFuture}。
 * <p>
 * 新版本使用代理实现，比较安全，也比较省心，不然老是要研究{@link CompletableFuture}的实现，还不能保证是否满足我的需求。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/12/30
 * github - https://github.com/hl845740757
 */
public class CompletableFutureAdapter<V> extends DelegateBlockingFuture<V> {

    public CompletableFutureAdapter(EventLoop defaultExecutor, CompletableFuture<V> future) {
        this(future, defaultExecutor.newBlockingPromise());
    }

    private CompletableFutureAdapter(CompletableFuture<V> future, BlockingPromise<V> promise) {
        super(promise);
        listen(future, promise);
    }

    /**
     * 这里并没有发布自己，而是发布的{@link BlockingPromise}对象
     */
    private static <V> void listen(CompletableFuture<V> future, BlockingPromise<V> promise) {
        future.whenComplete((v, throwable) -> {
            if (throwable != null) {
                promise.tryFailure(throwable);
            } else {
                promise.trySuccess(v);
            }
        });
    }

}
