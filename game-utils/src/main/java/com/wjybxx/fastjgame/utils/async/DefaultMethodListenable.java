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

package com.wjybxx.fastjgame.utils.async;

import com.wjybxx.fastjgame.utils.concurrent.FutureResult;
import com.wjybxx.fastjgame.utils.concurrent.ListenableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

/**
 * 方法结果监听器默认实现
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/6
 * github - https://github.com/hl845740757
 */
public class DefaultMethodListenable<FR extends FutureResult<V>, V> implements MethodListenable<FR, V> {

    private static final Logger logger = LoggerFactory.getLogger(DefaultMethodListenable.class);

    private FR futureResult;
    private GenericFutureResultListener<FR, V> listener;

    @Override
    public MethodListenable<FR, V> onSuccess(GenericSuccessFutureResultListener<FR, V> listener) {
        addListener(listener);
        return this;
    }

    @Override
    public MethodListenable<FR, V> onFailure(GenericFailureFutureResultListener<FR, V> listener) {
        addListener(listener);
        return this;
    }

    @Override
    public MethodListenable<FR, V> onComplete(GenericFutureResultListener<FR, V> listener) {
        addListener(listener);
        return this;
    }

    protected final void addListener(GenericFutureResultListener<FR, V> child) {
        if (futureResult != null) {
            // 关联的操作已完成
            notifyListenerSafely(futureResult, child);
            return;
        }

        if (this.listener == null) {
            // 多数情况下我们只有一个回调逻辑
            this.listener = child;
            return;
        }

        if (this.listener instanceof FutureResultListenerContainer) {
            ((FutureResultListenerContainer<FR, V>) this.listener).addChild(child);
        } else {
            this.listener = new FutureResultListenerContainer<>(this.listener, child);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public final void onComplete(ListenableFuture<V> future) throws Exception {
        futureResult = (FR) future.getAsResult();
        assert null != futureResult;
        if (null != listener) {
            notifyListenerSafely(futureResult, listener);
            listener = null;
        }
    }

    private static <FR extends FutureResult<V>, V> void notifyListenerSafely(@Nonnull FR fr, @Nonnull GenericFutureResultListener<FR, V> listener) {
        try {
            listener.onComplete(fr);
        } catch (Throwable e) {
            logger.warn("listener.onComplete caught exception", e);
        }
    }
}
