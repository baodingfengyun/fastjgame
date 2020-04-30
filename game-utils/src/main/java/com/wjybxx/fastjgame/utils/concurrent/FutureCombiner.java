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
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@link FluentFuture}聚合器。
 * FutureCombiner会将{@link #add(FluentFuture)} {@link #addAll(FluentFuture[])}方法添加的所有future的完成事件聚合为一个完成事件，
 * 并传递给{@link #finish(Promise)}方法指定的promise。
 * 即：当finish方法指定的promise进入完成状态时，在这前面添加的所有future都已进入了完成状态(如果外部没有赋值的话)。
 *
 * <p>
 * 在调用{@link #finish(Promise)}之前，用户可以通过{@link #add(FluentFuture)}和{@link #addAll(FluentFuture[])}方法添加任意数量的{@link FluentFuture}，
 * 当所有的future添加完毕之后，使用者需要调用{@link #finish(Promise)}方法指定接收所有future完成事件的promise。
 *
 * <h3>失败处理</h3>
 * 注意：当且仅当所有的future关联的操作都<b>成功完成</b>时{@link FluentFuture#isCompletedExceptionally() false}，{@link CombinerCompletion#aggregatePromise}才会表现为成功。
 * 一旦某一个future执行失败，则{@link CombinerCompletion#aggregatePromise}表现为失败。此外，如果多个future执行失败，
 * 那么{@link CombinerCompletion#aggregatePromise}最终接收到的{@link CombinerCompletion#cause}将是不确定的，并且不保证拥有所有的错误信息。
 *
 * <h3>非线程安全</h3>
 * 该实现不是线程安全的，只允许构造方法指定的{@link EventLoop}添加要监听的future。
 * <p>
 * <pre>{@code
 *      new FutureCombiner(eventLoop)
 *      .add(aFuture)
 *      .add(bFuture)
 *      .finish(FutureUtils.newPromise())
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

    private List<ListenableFuture<?>> futureList = new ArrayList<>(8);

    public FutureCombiner() {

    }

    public FutureCombiner addAll(@Nonnull FluentFuture<?>... futures) {
        for (FluentFuture<?> future : futures) {
            this.add(future);
        }
        return this;
    }

    public FutureCombiner addAll(@Nonnull Collection<? extends FluentFuture<?>> futures) {
        for (FluentFuture<?> future : futures) {
            this.add(future);
        }
        return this;
    }

    public FutureCombiner add(@Nonnull FluentFuture<?> future) {
        Objects.requireNonNull(future, "future");

        futureList.add(future);
        return this;
    }

    /**
     * 指定用于监听前面添加的所有future的完成事件的promise，该promise会在监听的所有future进入完成状态之后进入完成状态。
     *
     * @param aggregatePromise 用于监听前面的所有future的完成事件
     * @return 返回参数，方便直接添加回调。
     */
    public <T extends Promise<Void>> T finish(@Nonnull T aggregatePromise) {
        return combine(futureList, aggregatePromise);
    }

    /**
     * 清空{@link FutureCombiner}添加监听器，可以用于新的一轮合并。
     *
     * @return this
     */
    public FutureCombiner reset() {
        futureList.clear();
        return this;
    }

    /**
     * @param futures          所有要被监听的futures
     * @param aggregatePromise 用于接收所有future进入完成状态的promise
     * @return aggregatePromise，方便流式语法操作
     */
    public static <T extends Promise<Void>> T combine(Collection<ListenableFuture<?>> futures, @Nonnull T aggregatePromise) {
        Objects.requireNonNull(aggregatePromise, "aggregatePromise");
        if (futures.isEmpty()) {
            throw new IllegalStateException("Non futures");
        }

        final CombinerCompletion completion = new CombinerCompletion(futures.size(), aggregatePromise);
        for (ListenableFuture<?> future : futures) {
            future.addListener(completion);
        }

        return aggregatePromise;
    }

    /**
     * @param futures 所有要被监听的futures
     * @return promise 用于接收所有future进入完成状态的future
     */
    public static FluentFuture<Void> combine(ListenableFuture<?>... futures) {
        return combine(Arrays.asList(futures), FutureUtils.newPromise());
    }

    private static class CombinerCompletion implements FutureListener<Object> {

        final int expectedCount;
        final Promise<Void> aggregatePromise;

        final AtomicInteger doneCount = new AtomicInteger();
        Throwable cause;

        private CombinerCompletion(int expectedCount, Promise<Void> aggregatePromise) {
            this.expectedCount = expectedCount;
            this.aggregatePromise = aggregatePromise;
        }

        @Override
        public void onComplete(ListenableFuture<Object> future) throws Exception {
            final Throwable throwable = future.cause();
            if (throwable != null) {
                // 它可能被乱序赋值，我们并不需要保证是哪一个
                // 它的可见性又下面的CAS保证
                cause = throwable;
            }

            if (doneCount.incrementAndGet() == expectedCount) {
                // 这里能看见其它线程给cause的赋值
                if (cause == null) {
                    aggregatePromise.trySuccess(null);
                } else {
                    aggregatePromise.tryFailure(cause);
                }
            }
        }
    }
}
