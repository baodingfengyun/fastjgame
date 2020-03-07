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

import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.concurrent.Executor;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/3/5
 */
public class ExecutorBindListener<V> implements FutureListener<V> {

    public final FutureListener<V> listener;
    public final Executor bindExecutor;

    public ExecutorBindListener(FutureListener<V> listener, Executor bindExecutor) {
        this.listener = listener;
        this.bindExecutor = bindExecutor;
    }

    @Override
    public void onComplete(NListenableFuture<V> future) throws Exception {
        if (bindExecutor instanceof EventLoop && ((EventLoop) bindExecutor).inEventLoop()) {
            listener.onComplete(future);
        } else {
            bindExecutor.execute(() -> {
                try {
                    listener.onComplete(future);
                } catch (Exception e) {
                    ExceptionUtils.rethrow(e);
                }
            });
        }
    }
}
