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

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.Collection;
import java.util.Objects;

/**
 * {@link ListenableFuture}聚合器，它可以监听多个{@link ListenableFuture}的结果，并当所有监听的future进入完成状态时，
 * 通知{@link #finish(Promise)}方法指定的{@link Promise}，如果设置了的话。
 * <p>
 * 在调用{@link #finish(Promise)}之前，用户可以通过{@link #add(ListenableFuture)}和{@link #addAll(ListenableFuture[])}方法添加任意数量的{@link ListenableFuture}，
 * 当所有的future添加完毕之后，调用者需要调用{@link #finish(Promise)}方法监听最终的结果，如果需要的话。
 * <h3>失败处理</h3>
 * 注意：当且仅当所有的future关联的操作都<b>成功完成</b>时{@link ListenableFuture#isCompletedExceptionally() false}，{@link #aggregatePromise}才会表现为成功。
 * 一旦某一个future执行失败，则{@link #aggregatePromise}表现为失败。此外，如果多个future执行失败，
 * 那么{@link #aggregatePromise}最终接收到的{@link #cause}将是不确定的，并且不保证拥有所有的错误信息。
 *
 * <h3>非线程安全</h3>
 * 该实现不是线程安全的，只允许构造方法指定的{@link EventLoop}添加要监听的future。
 * <p>
 * <pre>{@code
 *      new FutureCombiner(eventLoop)
 *      .add(aFuture)
 *      .add(bFuture)
 *      .finish(eventLoop.newBlockingPromise())
 *      .addListener(f -> doSomething(f));
 * }</pre>
 *
 * <p>
 * Q: 它出现的必要性？
 * A: 当出现多个可并行执行的逻辑时，我们期望可以在所有操作完成时，执行一个操作(或进行一个通知)。
 * 当一个线程需要从多个线程拉取数据时，{@link FutureCombiner}就很有帮助。
 * <p>
 * Q: 如果将并行操作转换为多个单步操作有什么问题？
 * A: 如果每一步都是成功的，则没有问题，但是你必须处理失败的情况，每一步都需要负责是否继续下一步，是否进入完成状态(为promise赋值或通知监听器)。
 * 增加了每一步之间的耦合性，复杂度上升，如果某一步疏忽了错误处理，将导致监听器丢失信号。
 * 此外，也会导致一定的可读性下降。
 * </p>
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/15
 */
@NotThreadSafe
public class FutureCombiner {

    private final FutureListener<?> childrenListener = new ChildListener();
    private final EventLoop appEventLoop;

    private int expectedCount;
    private int doneCount;

    private Promise<Void> aggregatePromise;
    private Throwable cause;

    public FutureCombiner(EventLoop appEventLoop) {
        this.appEventLoop = appEventLoop;
    }

    public FutureCombiner addAll(@Nonnull ListenableFuture<?>... futures) {
        for (ListenableFuture<?> future : futures) {
            this.add(future);
        }
        return this;
    }

    public FutureCombiner addAll(@Nonnull Collection<? extends ListenableFuture<?>> futures) {
        for (ListenableFuture<?> future : futures) {
            this.add(future);
        }
        return this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public FutureCombiner add(@Nonnull ListenableFuture future) {
        Objects.requireNonNull(future, "future");
        checkInEventLoop("Adding future must be called from EventLoop thread");
        checkAddFutureAllowed();

        ++expectedCount;
        future.addListener(childrenListener, appEventLoop);
        return this;
    }

    private void checkInEventLoop(String msg) {
        EventLoopUtils.ensureInEventLoop(appEventLoop, msg);
    }

    private void checkAddFutureAllowed() {
        if (aggregatePromise != null) {
            throw new IllegalStateException("Adding futures is not allowed after finished adding");
        }
    }

    /**
     * 指定用于监听前面添加的所有future的完成事件的promise，该promise会在监听的所有future进入完成状态之后进入完成状态。
     *
     * @param aggregatePromise 用于监听前面的所有future的完成事件
     * @return 返回参数，方便直接添加回调。
     */
    public <T extends Promise<Void>> T finish(@Nonnull T aggregatePromise) {
        Objects.requireNonNull(aggregatePromise, "aggregatePromise");
        checkInEventLoop("Finish must be called from EventLoop thread");
        checkFinishAllowed();

        this.aggregatePromise = aggregatePromise;
        if (doneCount == expectedCount) {
            tryPromise();
        }

        return aggregatePromise;
    }

    private void checkFinishAllowed() {
        if (null != this.aggregatePromise) {
            throw new IllegalStateException("Already finished");
        }
    }

    private class ChildListener implements FutureListener<Object> {

        @Override
        public void onComplete(ListenableFuture<Object> future) throws Exception {
            assert appEventLoop.inEventLoop();

            if (future.isCompletedExceptionally()) {
                cause = future.cause();
            }

            doneCount++;

            if (doneCount == expectedCount && null != aggregatePromise) {
                tryPromise();
            }
        }
    }

    private boolean tryPromise() {
        return cause == null ? aggregatePromise.trySuccess(null) : aggregatePromise.tryFailure(cause);
    }

}
