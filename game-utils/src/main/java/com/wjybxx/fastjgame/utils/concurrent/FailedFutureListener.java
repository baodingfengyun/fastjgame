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

/**
 * 当future关联的操作执行失败时，该监听器才会执行。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/3/6
 */
@FunctionalInterface
public interface FailedFutureListener<V> extends FutureListener<V> {

    @Override
    default void onComplete(ListenableFuture<V> future) throws Exception {
        if (!future.isSuccess()) {
            onFailure(future);
        }
    }

    void onFailure(ListenableFuture<V> future) throws Exception;

}
