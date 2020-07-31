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

package com.wjybxx.fastjgame.net.utils;

import com.wjybxx.fastjgame.utils.concurrent.DefaultPromise;
import com.wjybxx.fastjgame.utils.concurrent.Promise;
import io.netty.util.concurrent.Future;

/**
 * 关于netty的一些适配实现
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/3/10
 */
public class NettyAdapters {

    private NettyAdapters() {

    }

    /**
     * 代理netty的future实现 - 双向监听。
     *
     * @param future 需要被代理的netty的future
     */
    public static <V> Promise<V> delegateFuture(Future<V> future) {
        final Promise<V> promise = new NettyPromise<>(future);

        future.addListener(f -> {
            if (f.isSuccess()) {
                @SuppressWarnings("unchecked") final V result = (V) f.getNow();
                promise.trySuccess(result);
            } else {
                promise.tryFailure(f.cause());
            }
        });

        return promise;
    }

    private static class NettyPromise<V> extends DefaultPromise<V> {

        final Future<V> future;

        private NettyPromise(Future<V> future) {
            this.future = future;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return future.cancel(mayInterruptIfRunning) && super.cancel(mayInterruptIfRunning);
        }

    }
}
