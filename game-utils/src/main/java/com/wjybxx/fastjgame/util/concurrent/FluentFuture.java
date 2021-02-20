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

package com.wjybxx.fastjgame.util.concurrent;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.*;

/**
 * 该接口对标{@link CompletionStage}，多数接口意义一样，但有一定区别。
 * 1. 部分API放在这里并不合适，会导致接口膨胀，因此该类的API类型是少于{@link CompletionStage}的。
 * 2. 去掉不带{@link java.util.concurrent.Executor}的async系列方法。
 * 3. 增加了对{@link Callable}的支持 - {@link Supplier#get()}较为特殊，因此选{@link Callable}。
 * 4. {@link #catching(Class, Function)} {@link #whenComplete(BiConsumer)} {@link #whenExceptionally(Consumer)}有别于{@link CompletionStage}。
 * <p>
 * Q: 为什么放弃了之前的{@code ListenableFuture} ?
 * A: 最近在项目里实现了一套流式监听回调机制(单线程版)，参考了{@link CompletableFuture}的函数式语法实现，发现太强了，用着真的上瘾，
 * 尤其是{@link CompletionStage#thenCompose(Function)}方法，可以消除嵌套回调，可以非常好的表达业务，而简单的{@code addListener}则差太多了。
 * 于是我决定回归JDK的{@link CompletableFuture}。
 * <p>
 * Q: 为什么在底层自动记录异常日志了？
 * A: 发现如果靠写业务的时候保证不丢失异常信息，十分危险，如果疏忽将导致异常信息丢失，异常信息十分重要，不可丢失。
 * 主要是使用Rpc等工具的地方太多了，每一处都得异常，会带来很大的心里负担。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/3/6
 */
@ThreadSafe
public interface FluentFuture<V> extends ListenableFuture<V> {

    /**
     * {@inheritDoc}
     * <p>链式操作，使得取消变得困难，因此在上面的取消操作可能达不到预期。
     */
    @Override
    boolean cancel(boolean mayInterruptIfRunning);

    /**
     * 该方法表示在当前{@code Future}与返回的{@code Future}中插入一个异步操作，构建异步管道。
     * 这是本类的核心API，该方法非常强大，可以避免嵌套回调(回调地狱)。
     * 该方法对应我们日常流中使用的{@link java.util.stream.Stream#flatMap(Function)}操作。
     * <p>
     * 该方法返回一个新的{@code Future}，它的最终结果与指定的{@code Function}返回的{@code Future}结果相同。
     * 如果当前{@code Future}执行失败，则返回的{@code Future}将以相同的原因失败，且指定的动作不会执行。
     * 如果当前{@code Future}执行成功，则当前{@code Future}的执行结果将作为指定操作的执行参数。
     * <p>
     * {@link CompletionStage#thenCompose(Function)}
     */
    <U> FluentFuture<U> thenCompose(@Nonnull Function<? super V, ? extends ListenableFuture<U>> fn);

    <U> FluentFuture<U> thenComposeAsync(@Nonnull Function<? super V, ? extends ListenableFuture<U>> fn, Executor executor);

    /**
     * {@link #thenCompose(Function)}的特化版本，主要用于消除丑陋的void
     * <p>
     * 该方法返回一个新的{@code Future}，它的最终结果与指定的{@code Callable}返回的{@code Future}结果相同。
     * 如果当前{@code Future}执行失败，则返回的{@code Future}将以相同的原因失败，且指定的动作不会执行。
     * 如果当前{@code Future}执行成功，则执行指定的操作。
     * <p>
     * 该方法在{@link CompletionStage}中也是不存在的。
     */
    <U> FluentFuture<U> thenCompose(@Nonnull Callable<? extends ListenableFuture<U>> fn);

    <U> FluentFuture<U> thenComposeAsync(@Nonnull Callable<? extends ListenableFuture<U>> fn, Executor executor);
    ///////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * 返回一个新的{@code Future}，它的结果由当前{@code Future}驱动。
     * 如果当前{@code Future}执行失败，则返回的{@code Future}将以相同的原因失败，且指定的动作不会执行。
     * 如果当前{@code Future}执行成功，则执行给定的操作，返回的{@code Future}的结果取决于指定操作的执行结果。
     * <p>
     * {@link CompletionStage#thenRun(Runnable)}
     */
    FluentFuture<Void> thenRun(@Nonnull Runnable action);

    FluentFuture<Void> thenRunAsync(@Nonnull Runnable action, Executor executor);

    /**
     * 该方法返回一个新的{@code Future}，它的结果由当前{@code Future}驱动。
     * 如果当前{@code Future}执行失败，则返回的{@code Future}将以相同的原因失败，且指定的动作不会执行。
     * 如果当前{@code Future}执行成功，则当前{@code Future}的执行结果将作为指定操作的执行参数，返回的{@code Future}的结果取决于指定操作的执行结果。
     * <p>
     * 这个API在{@link CompletionStage}中是没有对应方法的。
     * 由于{@link Supplier#get()}方法名太特殊，{@code thenSupply}也不是个好名字，因此使用{@link Callable#call()}。
     */
    <U> FluentFuture<U> thenCall(@Nonnull Callable<U> fn);

    <U> FluentFuture<U> thenCallAsync(@Nonnull Callable<U> fn, Executor executor);

    /**
     * 该方法返回一个新的{@code Future}，它的结果由当前{@code Future}驱动。
     * 如果当前{@code Future}执行失败，则返回的{@code Future}将以相同的原因失败，且指定的动作不会执行。
     * 如果当前{@code Future}执行成功，则当前{@code Future}的执行结果将作为指定操作的执行参数，返回的{@code Future}的结果取决于指定操作的执行结果。
     * <p>
     * {@link CompletionStage#thenAccept(Consumer)}
     */
    FluentFuture<Void> thenAccept(@Nonnull Consumer<? super V> action);

    FluentFuture<Void> thenAcceptAsync(@Nonnull Consumer<? super V> action, Executor executor);

    /**
     * 该方法返回一个新的{@code Future}，它的结果由当前{@code Future}驱动。
     * 如果当前{@code Future}执行失败，则返回的{@code Future}将以相同的原因失败，且指定的动作不会执行。
     * 如果当前{@code Future}执行成功，则当前{@code Future}的执行结果将作为指定操作的执行参数，返回的{@code Future}的结果取决于指定操作的执行结果。
     * <p>
     * {@link CompletionStage#thenApply(Function)}
     */
    <U> FluentFuture<U> thenApply(@Nonnull Function<? super V, ? extends U> fn);

    <U> FluentFuture<U> thenApplyAsync(@Nonnull Function<? super V, ? extends U> fn, Executor executor);
    ///////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * 它表示能从从特定的异常中恢复，并返回一个正常结果。
     * <p>
     * 该方法返回一个新的{@code Future}，它的结果由当前{@code Future}驱动。
     * 如果当前{@code Future}正常完成，则给定的动作不会执行，且返回的{@code Future}使用相同的结果值进入完成状态。
     * 如果当前{@code Future}执行失败，如果异常可处理，则将其异常信息将作为指定操作的执行参数，返回的{@code Future}的结果取决于指定操作的执行结果，
     * 否则，返回的{@code Future}以相同的的异常进入完成状态。
     * <p>
     * 不得不说JDK的{@link CompletionStage#exceptionally(Function)}这个名字太差劲了，实现的也不够好，因此我们不使用它，这里选择了Guava中的实现。
     *
     * @param exceptionType 能处理的异常类型
     * @param fallback      异常恢复函数
     */
    <X extends Throwable>
    FluentFuture<V> catching(@Nonnull Class<X> exceptionType, @Nonnull Function<? super X, ? extends V> fallback);

    <X extends Throwable>
    FluentFuture<V> catchingAsync(@Nonnull Class<X> exceptionType, @Nonnull Function<? super X, ? extends V> fallback, Executor executor);

    /**
     * 该方法表示既能处理当前计算的正常结果，又能处理当前结算的异常结果(可以将异常转换为新的结果)，并返回一个新的结果。
     * 该方法可能隐藏异常，使用者一定注意。
     * <p>
     * 该方法返回一个新的{@code Future}，无论当前{@code Future}执行成功还是失败，给定的操作都将执行。
     * 如果当前{@code Future}执行成功，而指定的动作出现异常，则返回的{@code Future}以该异常完成。
     * 如果当前{@code Future}执行失败，且指定的动作出现异常，则返回的{@code Future}以新抛出的异常进入完成状态。
     * (也就是说，一旦给定动作出现异常，返回的{@code Future}都将以新抛出的异常进入完成状态)
     * <p>
     * {@link CompletionStage#handle(BiFunction)}
     */
    <U> FluentFuture<U> thenHandle(@Nonnull BiFunction<? super V, ? super Throwable, ? extends U> fn);

    <U> FluentFuture<U> thenHandleAsync(@Nonnull BiFunction<? super V, ? super Throwable, ? extends U> fn, Executor executor);

    ///////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * 该方法表示消费者希望能消费一遍当前{@code Future}的执行结果，并将结果传递给下一个{@code Future}。
     * <p>
     * 该方法返回一个新的{@code Future}，它的结果始终与当前{@code Future}的结果相同。
     * <p>
     * 与方法{@link #thenHandle(BiFunction)}不同，此方法不是为转换完成结果而设计的，因此提供的操作<b>不应引发异常</b>，
     * 如果确实引发的了异常，则仅仅记录一个<b>日志</b>，不会向下传递，这里与JDK的{@link CompletionStage#whenComplete(BiConsumer)}并不相同。
     * <p>
     * {@link CompletionStage#whenComplete(BiConsumer)}
     */
    FluentFuture<V> whenComplete(@Nonnull BiConsumer<? super V, ? super Throwable> action);

    FluentFuture<V> whenCompleteAsync(@Nonnull BiConsumer<? super V, ? super Throwable> action, Executor executor);

    /**
     * 该方法返回一个新的{@code Future}，它的结果始终与当前{@code Future}的结果相同。
     * <p>
     * 该方法表示希望消费一遍当前{@code Future}的执行结果，并将异常直接传递给下一个{@code Future}
     * <p>
     * 与{@link #catching(Class, Function)}不同，此方法不是为转换完成结果而设计的，因此提供的操作<b>不应引发异常</b>，
     * 如果确实引发的了异常，则仅仅记录一个<b>日志</b>，不会向下传递。
     * <p>
     * 该方法在{@link CompletionStage}中是不存在对应方法的。
     */
    FluentFuture<V> whenExceptionally(@Nonnull Consumer<? super Throwable> action);

    FluentFuture<V> whenExceptionallyAsync(@Nonnull Consumer<? super Throwable> action, Executor executor);

    // ------------------------------------- 语法支持  --------------------------------------

    @Override
    FluentFuture<V> await() throws InterruptedException;

    @Override
    FluentFuture<V> awaitUninterruptibly();

    @Override
    FluentFuture<V> addListener(@Nonnull FutureListener<? super V> listener);

    @Override
    FluentFuture<V> addListener(@Nonnull FutureListener<? super V> listener, @Nonnull Executor executor);

}
