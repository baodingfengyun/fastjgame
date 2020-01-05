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

package com.wjybxx.fastjgame.concurrent.async;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 为多个异步方法回调提供单个视图。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/1/5 20:23
 * github - https://github.com/hl845740757
 */
class CompositeAsyncMethodCallback<V> implements AsyncMethodCallback<V> {

    private static final Logger logger = LoggerFactory.getLogger(CompositeAsyncMethodCallback.class);

    private final List<AsyncMethodCallback<? super V>> children = new ArrayList<>(2);

    CompositeAsyncMethodCallback(AsyncMethodCallback<? super V> first, AsyncMethodCallback<? super V> second) {
        children.add(first);
        children.add(second);
    }

    void addChild(AsyncMethodCallback<? super V> child) {
        children.add(child);
    }

    @Override
    public void onComplete(AsyncMethodResult<? extends V> asyncMethodResult) {
        for (AsyncMethodCallback<? super V> child : children) {
            try {
                child.onComplete(asyncMethodResult);
            } catch (Throwable ex) {
                final Throwable cause = asyncMethodResult.getCause();
                if (cause != null) {
                    cause.addSuppressed(ex);
                }
                logger.warn("Child.onComplete caught exception!", cause != null ? cause : ex);
            }
        }
    }
}
