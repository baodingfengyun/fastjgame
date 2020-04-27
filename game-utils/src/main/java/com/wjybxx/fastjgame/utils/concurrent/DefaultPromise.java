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

import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Executor;

/**
 * 默认的{@link Promise}实现。
 * 它使用锁来管理监听器，因此较为重量级（功力有限）。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/14
 * github - https://github.com/hl845740757
 */
public class DefaultPromise<V> extends AbstractPromise<V> {

    /**
     * 该future上注册的监听器们。
     * 我们使用Null表示一个监听器也没有的状态，每次通知时都会置为null。
     */
    @GuardedBy("this")
    private Completion listeners = null;
    /**
     * 当前是否有线程正在通知监听器们。我们必须阻止并发的通知 和 保持监听器的先入先出顺序(先添加的先被通知)。
     */
    @GuardedBy("this")
    private boolean notifyingListeners = false;

    public DefaultPromise() {

    }

    public DefaultPromise(@Nullable Executor workerExecutor) {
        super(workerExecutor);
    }

    @Override
    protected <U> AbstractPromise<U> newIncompletePromise() {
        return new DefaultPromise<>(getWorkerExecutor());
    }

    @Override
    protected void notifyListeners() {
        notifyAllListenersNow();
    }

    private void notifyAllListenersNow() {
        // 用于拉取最新的监听器，避免长时间的占有锁
        Completion tmpListeners;
        synchronized (this) {
            // 有线程正在进行通知 或当前 没有监听器，则不需要当前线程进行通知
            if (notifyingListeners || null == this.listeners) {
                return;
            }
            // 标记为正在通知(避免并发通知，当前正在通知的线程，会通知所有的监听器)
            notifyingListeners = true;
            tmpListeners = this.listeners;
            this.listeners = null;
        }

        for (; ; ) {
            // 通知当前批次的监听器(此时不需要获得锁) -- 但是这里不能抛出异常，否则可能死锁(notifyingListeners状态无法清除)
            tmpListeners.onComplete();

            // 通知完当前批次后，检查是否有新的监听器加入
            synchronized (this) {
                if (null == this.listeners) {
                    // 通知完毕
                    this.notifyingListeners = false;
                    break;
                }
                // 有新的监听器加入，拉取最新的监听器，继续通知 -- 可以保证被通知的顺序
                tmpListeners = this.listeners;
                this.listeners = null;
            }
        }
    }

    @Override
    protected void pushCompletion(Completion completion) {
        pushCompletionImpl(completion);

        // 如果已进入完成状态，通知刚刚加入监听器们（必须检查完成状态，否则可能丢失通知）
        if (isDone()) {
            notifyAllListenersNow();
        }
    }

    private void pushCompletionImpl(Completion completion) {
        // 不管是否已完成，先加入等待通知集合，必须保证执行时序
        synchronized (this) {
            if (listeners == null) {
                listeners = completion;
                return;
            }
            if (listeners instanceof DefaultPromise.CompositeCompletion) {
                ((CompositeCompletion) listeners).addChild(completion);
            } else {
                listeners = new CompositeCompletion(listeners, completion);
            }
        }
    }

    private static class CompositeCompletion extends Completion {

        /**
         * Q: 为什么使用数组？为了解决什么问题？
         * A: 主要解决{@link ArrayList}在初始容量较小时扩容频繁的问题，
         * {@link ArrayList}默认扩容50%，初始容量为2，那么容量扩充节奏为：2，3，4，6，9， 对于小容量容器非常不友好。
         * 而如果扩容100%，如果初始容量为2，那么容量扩充节奏为： 2，4，8，对于小容量容器更好。
         */
        private Completion[] children;
        private int size;

        CompositeCompletion(Completion first, Completion second) {
            children = new Completion[2];
            size = 2;

            children[0] = first;
            children[1] = second;
        }

        void addChild(Completion child) {
            ensureCapacity();
            children[size++] = child;
        }

        private void ensureCapacity() {
            if (size == children.length) {
                children = Arrays.copyOf(children, size << 1);
            }
        }

        @Override
        protected void onComplete() {
            for (Completion completion : children) {
                completion.onComplete();
            }
        }
    }
}
