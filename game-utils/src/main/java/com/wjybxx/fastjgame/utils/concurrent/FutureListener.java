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

package com.wjybxx.fastjgame.utils.concurrent;


/**
 * Future的监听者。
 * <p>
 * 监听{@link BlockingFuture}的结果。一旦该listener通过{@link BlockingFuture#addListener(FutureListener)}添加到future上，
 * future上关联的异步操作完成时，就会收到通知。
 *
 * <pre>{@code
 * ListenableFuture f = submit(task)
 * f.addListener(future -> doSomething());
 * }</pre>
 *
 * @param <V> Listener期望消费的类型，也是Future生产的类型。
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/14
 * github - https://github.com/hl845740757
 */
@FunctionalInterface
public interface FutureListener<V> {

    /**
     * 当监听的Future对应的操作完成时，该方法将会被调用。
     * 在回调代码中：
     * 如果有返回值，建议使用{@link BlockingFuture#getNow()}。
     * 如果没有返回值，建议使用{@link BlockingFuture#isSuccess()}。
     *
     * @param future 监听器监听的future。
     * @throws Exception error
     */
    void onComplete(ListenableFuture<V> future) throws Exception;

}
