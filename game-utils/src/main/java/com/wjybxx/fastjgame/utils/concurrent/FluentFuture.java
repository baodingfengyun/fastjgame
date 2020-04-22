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

import javax.annotation.Nonnull;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 虽然{@code FluentFuture}的名字借鉴了Guava中的名字，但是采用和{@link CompletionStage}相同的api。
 * 其实是为了和{@link CompletionStage}达成相同的目的，由于{@link CompletionStage}太重量级，容易消化不良，因此我们不采用它。
 * 此外，{@link CompletionStage}的回调执行顺序我们是无法保证的，我们需要更强的回调执行顺序保证。
 * <p>
 * 由于和{@link CompletionStage}使用相同的api，因此方法归类也是相同。即：
 * 命名包含{@code handle} {@code when}的方法，无论{@code Future}执行成功还是失败，给定动作都将执行。
 * 而其它方法只有{@code Future}正常完成时，才会执行给定动作。
 *
 * <p>
 * Q: 为什么要引入该类，该类为了解决什么问题？
 * A: 该类主要为了解决异步操作流的问题。举个栗子：
 * 假设我们现在有一个业务，需要3步才能完成，且每一步都是异步的，其执行过程为： A -> B -> C。
 * 按照老式的代码，其代码将是这样的：
 * <pre> {@code
 *   void stepA() {
 *       doSomethingA()
 *       .addListener(this::stepB);
 *   }
 *
 *   void stepB(ListenableFuture future) {
 *      doSomethingB(future.getNow())
 *      .addListener(this::stepC);
 *   }
 *
 *   void stepC(ListenableFuture future){
 *       doSomethingC(future.getNow())
 *       .addListener(this::onComplete);
 *   }
 * }
 * </pre>
 * 其主要问题是执行流程过于分散，很难表现全局执行流程，且每一步之间强耦合，而如果使用{@link FluentFuture#thenCompose(Function)}，
 * 那么以上代码可能是这样的：
 * <pre>{@code
 *     void execute() {
 *          FluentFutures.form(stepA) // 假设的一个转换方法(将普通的Future转换为FluentFuture，很容易实现)
 *          .thenCompose(this::stepB)
 *          .thenCompose(this::stepC)
 *          .addListener(this::onComplete);
 *     }
 *
 *     ListenableFuture stepA() {
 *          return doSomethingA();
 *     }
 *
 *     ListenableFuture stepB(Object aResult) {
 *          return doSomethingB(aResult);
 *     }
 *
 *     ListenableFuture stepC(Object bResult) {
 *          return doSomethingC(bResult);
 *     }
 * }</pre>
 * 这样的代码具有更好的可读性和全局观。
 * 其实{@link FluentFuture}的核心API其实就一个{@link #thenCompose(Function)}，其它的API仅仅是锦上添花。
 *
 * <p>
 * Q: 这些API为什么出现在子类里，而不是{@link ListenableFuture}中？
 * A: 那样会使得{@link ListenableFuture}太重量级，而且很多时候我们可能并不需要那么多的API。
 * (现在的{@link java.util.concurrent.CompletableFuture}类职责就很多)
 *
 * <p>
 * 注意：实现应该提供方便的静态方法包装普通的{@link ListenableFuture}。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/4/22
 */
public interface FluentFuture<V> extends ListenableFuture<V> {

    /**
     * 该方法返回一个新的{@code Future}，它的最终结果与指定的{@code Function}返回的{@code Future}结果相同。
     * 如果当前{@code Future}执行失败，则返回的{@code Future}将以相同的原因失败，且指定的动作不会执行。
     * 如果当前{@code Future}执行成功，则当前{@code Future}的执行结果将作为指定操作的执行参数。
     * <p>
     * 这是本类的核心API，它的主要作用是构建异步管道，该方法非常强大，一定要熟悉之后再使用。
     * <p>
     * {@link CompletionStage#thenCompose(Function)}
     */
    <U> FluentFuture<U> thenCompose(Function<? super V, ? extends ListenableFuture<U>> fn);

    /**
     * {@inheritDoc}
     * 链式操作，使得取消变得困难。
     */
    @Override
    boolean cancel(boolean mayInterruptIfRunning);

    // ---------------------------------------- 以下只是锦上添花的功能而已 ---------------------------------

    /**
     * 返回一个新的{@code Future}，它的结果由当前{@code Future}驱动。
     * 如果当前{@code Future}执行失败，则返回的{@code Future}将以相同的原因失败，且指定的动作不会执行。
     * 如果当前{@code Future}执行成功，则执行给定的操作，返回的{@code Future}的结果取决于指定操作的执行结果。
     * <p>
     * {@link CompletionStage#thenRun(Runnable)}
     */
    FluentFuture<Void> thenRun(Runnable action);

    /**
     * 该方法返回一个新的{@code Future}，它的结果由当前{@code Future}驱动。
     * 如果当前{@code Future}执行失败，则返回的{@code Future}将以相同的原因失败，且指定的动作不会执行。
     * 如果当前{@code Future}执行成功，则当前{@code Future}的执行结果将作为指定操作的执行参数，返回的{@code Future}的结果取决于指定操作的执行结果。
     * <p>
     * {@link CompletionStage#thenAccept(Consumer)}
     */
    FluentFuture<Void> thenAccept(Consumer<? super V> action);

    /**
     * 该方法返回一个新的{@code Future}，它的结果由当前{@code Future}驱动。
     * 如果当前{@code Future}执行失败，则返回的{@code Future}将以相同的原因失败，且指定的动作不会执行。
     * 如果当前{@code Future}执行成功，则当前{@code Future}的执行结果将作为指定操作的执行参数，返回的{@code Future}的结果取决于指定操作的执行结果。
     * <p>
     * {@link CompletionStage#thenApply(Function)}
     */
    <U> FluentFuture<U> thenApply(Function<? super V, ? extends U> fn);

    /**
     * 该方法返回一个新的{@code Future}，无论当前{@code Future}执行成功还是失败，给定的操作都将执行。
     * 与方法{@link #thenHandle(BiFunction)}不同，此方法不是为转换完成结果而设计的，因此提供的操作不应引发异常。
     * 但是，如果确实引发的了异常，则应用以下规则：
     * 如果当前{@code Future}执行成功，而指定的动作出现异常，则返回的{@code Future}以该异常完成。
     * 如果当前{@code Future}执行失败，且指定的动作出现异常，则返回的{@code Future}以当前{@code Future}的异常完成(新的异常会被压入)。
     * <p>
     * 如果用在最后一步的话，可以考虑使用{@link #addListener(FutureListener)}代替该方法。
     * <p>
     * {@link CompletionStage#whenComplete(BiConsumer)}
     */
    FluentFuture<V> whenComplete(BiConsumer<? super V, ? super Throwable> action);

    /**
     * 该方法返回一个新的{@code Future}，无论当前{@code Future}执行成功还是失败，给定的操作都将执行。
     * 该方法可以将异常转换为新的结果。
     * <p>
     * {@link CompletionStage#handle(BiFunction)}
     */
    <U> FluentFuture<U> thenHandle(BiFunction<? super V, ? super Throwable, ? extends U> fn);

    // 普通流式语法支持
    @Override
    FluentFuture<V> addListener(@Nonnull FutureListener<? super V> listener);

    @Override
    FluentFuture<V> addListener(@Nonnull FutureListener<? super V> listener, @Nonnull Executor bindExecutor);
}