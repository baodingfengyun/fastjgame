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
import javax.annotation.concurrent.ThreadSafe;
import java.util.concurrent.*;
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
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/3/6
 */
@ThreadSafe
public interface FluentFuture<V> extends Future<V> {

    /**
     * 查询future关联的任务是否可以被取消。
     *
     * @return true/false 当且仅当future关联的任务可以通过{@link #cancel(boolean)}被取消时返回true。
     */
    boolean isCancellable();

    /**
     * 尝试取消future关联的任务，如果取消成功，会使得Future进入完成状态，并且{@link #cause()}将返回{@link CancellationException}。
     * 1. 如果取消成功，则返回true。
     * 2. 如果任务已经被取消，则返回true。
     * 3. 如果future关联的任务已完成，则返回false。
     * <p>
     * 链式操作，使得取消变得困难，因此在上面的取消操作可能达不到预期。
     *
     * @param mayInterruptIfRunning 是否允许中断工作者线程，在Future/Promise模式下意义不大
     * @return 是否取消成功
     */
    boolean cancel(boolean mayInterruptIfRunning);

    /**
     * 如果future以任何形式的异常完成（包括被取消)，则返回true。
     * 1. 如果future尚未进入完成状态，则返回false。
     * 2. 如果任务正常完成，则返回false。
     * 3. 当future关联的任务被取消或由于异常进入完成状态，则返回true。
     * <p>
     * 如果返回true，则{@link #cause()}不为null，也就是说该方法等价于{@code cause() != null}。
     */
    boolean isCompletedExceptionally();

    /**
     * 非阻塞获取导致任务失败的原因。
     * 1. 如果future关联的task还未进入完成状态{@link #isDone() false}，则返回null。
     * 2. 如果future关联的task已正常完成，则返回null。
     * 3. 当future关联的任务被取消或由于异常进入完成状态后，该方法将返回操作失败的原因。
     *
     * @return 失败的原因
     */
    Throwable cause();

    /**
     * 非阻塞的获取当前结果：
     * 1. 如果future关联的task还未完成{@link #isDone() false}，则返回null。
     * 2. 如果任务执行成功，则返回对应的结果。
     * 2. 如果任务被取消或失败，则抛出对应的异常。
     * <p>
     * 注意：
     * 如果future关联的task没有返回值(操作完成返回null)，此时不能根据返回值做任何判断。对于这种情况，
     * 你可以使用{@link #isCompletedExceptionally()},作为更好的选择。
     *
     * @return task执行结果
     * @throws CancellationException 如果任务被取消了，则抛出该异常
     * @throws CompletionException   如果在计算过程中出现了其它异常导致任务失败，则抛出该异常。
     */
    V getNow();

    // ------------------------------------- 阻塞式API --------------------------------------

    /**
     * 阻塞式获取task的结果，阻塞期间不响应中断。
     * 如果future关联的task尚未完成，则阻塞等待至任务完成，并返回计算的结果。
     * 如果future关联的task已完成，则立即返回结果。
     * <p>
     * 注意：
     * 如果future关联的task没有返回值(操作完成返回null)，此时不能根据返回值做任何判断。对于这种情况，
     * 你可以使用{@link #isCompletedExceptionally()},作为更好的选择。
     *
     * @return task的结果
     * @throws CancellationException 如果任务被取消了，则抛出该异常
     * @throws CompletionException   如果在计算过程中出现了其它异常导致任务失败，则抛出该异常。
     *                               使用非受检异常更好，这里和{@link CompletableFuture#join()}是一样的。
     */
    V join();

    /**
     * 在指定的时间范围内等待，直到future关联的任务进入完成状态。
     * 如果正常返回，接下来的{@link #isDone()}调用都将返回true。
     *
     * @param timeout 等待的最大时间，<b>如果小于等于0，表示不阻塞</b>
     * @param unit    时间单位
     * @return 当且仅当future关联的task在指定时间内进入了完成状态，返回true。也就是接下来的{@link #isDone() true} 。
     * @throws InterruptedException 如果当前线程在等待期间被中断
     */
    boolean await(long timeout, @Nonnull TimeUnit unit) throws InterruptedException;

    /**
     * 在指定的时间范围内等待，直到future关联的任务进入完成状态，并且在等待期间不响应中断。
     * 在等待期间，会捕获中断，并默默的丢弃，在方法返回前会恢复中断状态。
     *
     * @param timeout 等待的最大时间，<b>如果小于等于0，表示不阻塞</b>
     * @param unit    时间单位
     * @return 当且仅当future关联的任务，在特定时间范围内进入完成状态时返回true。也就是接下来的{@link #isDone() true}。
     */
    boolean awaitUninterruptibly(long timeout, @Nonnull TimeUnit unit);

    /**
     * 等待future进入完成状态。
     * await()不会查询任务的结果，在Future进入完成状态之后就返回，方法返回后，接下来的{@link #isDone()}调用都将返回true。
     *
     * @return this
     * @throws InterruptedException 如果在等待期间线程被中断，则抛出中断异常。
     */
    FluentFuture<V> await() throws InterruptedException;

    /**
     * 等待future进入完成状态，等待期间不响应中断，并默默的丢弃，在方法返回前会重置中断状态。
     * 在方法返回之后，接下来的{@link #isDone()}调用都将返回true。
     *
     * @return this
     */
    FluentFuture<V> awaitUninterruptibly();

    // ------------------------------------- 监听器相关(计算结果处理) --------------------------------------

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
    <U> FluentFuture<U> thenCompose(@Nonnull Function<? super V, ? extends FluentFuture<U>> fn);

    /**
     * {@link #thenCompose(Function)}的特化版本，主要用于消除丑陋的void
     * <p>
     * 该方法返回一个新的{@code Future}，它的最终结果与指定的{@code Function}返回的{@code Future}结果相同。
     * 如果当前{@code Future}执行失败，则返回的{@code Future}将以相同的原因失败，且指定的动作不会执行。
     * 如果当前{@code Future}执行成功，则执行指定从操作。
     * <p>
     * 该方法在{@link CompletionStage}中也是不存在的。
     */
    <U> FluentFuture<U> thenCompose(@Nonnull Callable<? extends FluentFuture<U>> fn);

    ///////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * 返回一个新的{@code Future}，它的结果由当前{@code Future}驱动。
     * 如果当前{@code Future}执行失败，则返回的{@code Future}将以相同的原因失败，且指定的动作不会执行。
     * 如果当前{@code Future}执行成功，则执行给定的操作，返回的{@code Future}的结果取决于指定操作的执行结果。
     * <p>
     * {@link CompletionStage#thenRun(Runnable)}
     */
    FluentFuture<Void> thenRun(@Nonnull Runnable action);

    /**
     * 该方法返回一个新的{@code Future}，它的结果由当前{@code Future}驱动。
     * 如果当前{@code Future}执行失败，则返回的{@code Future}将以相同的原因失败，且指定的动作不会执行。
     * 如果当前{@code Future}执行成功，则当前{@code Future}的执行结果将作为指定操作的执行参数，返回的{@code Future}的结果取决于指定操作的执行结果。
     * <p>
     * 这个API在{@link CompletionStage}中是没有对应方法的。
     * 由于{@link Supplier#get()}方法名太特殊，因此使用{@link Callable#call()}。
     */
    <U> FluentFuture<U> thenCall(@Nonnull Callable<U> fn);

    /**
     * 该方法返回一个新的{@code Future}，它的结果由当前{@code Future}驱动。
     * 如果当前{@code Future}执行失败，则返回的{@code Future}将以相同的原因失败，且指定的动作不会执行。
     * 如果当前{@code Future}执行成功，则当前{@code Future}的执行结果将作为指定操作的执行参数，返回的{@code Future}的结果取决于指定操作的执行结果。
     * <p>
     * {@link CompletionStage#thenAccept(Consumer)}
     */
    FluentFuture<Void> thenAccept(@Nonnull Consumer<? super V> action);

    /**
     * 该方法返回一个新的{@code Future}，它的结果由当前{@code Future}驱动。
     * 如果当前{@code Future}执行失败，则返回的{@code Future}将以相同的原因失败，且指定的动作不会执行。
     * 如果当前{@code Future}执行成功，则当前{@code Future}的执行结果将作为指定操作的执行参数，返回的{@code Future}的结果取决于指定操作的执行结果。
     * <p>
     * {@link CompletionStage#thenApply(Function)}
     */
    <U> FluentFuture<U> thenApply(@Nonnull Function<? super V, ? extends U> fn);

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

    /**
     * 该方法表示既能处理当前计算的正常结果，又能处理当前结算的异常结果(可以将异常转换为新的结果)，并返回一个新的结果。
     * 该方法可能隐藏异常，使用者一定注意。
     * <p>
     * 该方法返回一个新的{@code Future}，无论当前{@code Future}执行成功还是失败，给定的操作都将执行。
     * 如果当前{@code Future}执行成功，而指定的动作出现异常，则返回的{@code Future}以该异常完成。
     * 如果当前{@code Future}执行失败，且指定的动作出现异常，则返回的{@code Future}以新抛出的异常进入完成状态。
     * <p>
     * {@link CompletionStage#handle(BiFunction)}
     */
    <U> FluentFuture<U> thenHandle(@Nonnull BiFunction<? super V, ? super Throwable, ? extends U> fn);

    // ------------------------------------- 用在末尾的方法  --------------------------------------

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

    void addListener(FutureListener<? super V> listener);

    void addListener(FutureListener<? super V> listener, Executor executor);

    // ------------------------------------- 一个特殊的API  --------------------------------------

    /**
     * 如果{@code Future}已进入完成状态，则立即执行给定动作，否则什么也不做。
     * <p>
     * 我现在理解{@link CompletionStage#toCompletableFuture()}方法为什么必须存在了，部分操作依赖于{@link CompletableFuture}的实现。
     * 不过我换了种方式，采用的是访问者的方式，这样可以减少对具体实现的依赖。
     * <p>
     * 该API用户也可以使用。
     *
     * @param action 用于接收当前的执行结果
     */
    void acceptNow(@Nonnull BiConsumer<? super V, ? super Throwable> action);

    // ------------------------------------- 异步版本  --------------------------------------

    <U> FluentFuture<U> thenComposeAsync(@Nonnull Function<? super V, ? extends FluentFuture<U>> fn, Executor executor);

    <U> FluentFuture<U> thenComposeAsync(@Nonnull Callable<? extends FluentFuture<U>> fn, Executor executor);

    FluentFuture<Void> thenRunAsync(@Nonnull Runnable action, Executor executor);

    <U> FluentFuture<U> thenCallAsync(@Nonnull Callable<U> fn, Executor executor);

    FluentFuture<Void> thenAcceptAsync(@Nonnull Consumer<? super V> action, Executor executor);

    <U> FluentFuture<U> thenApplyAsync(@Nonnull Function<? super V, ? extends U> fn, Executor executor);

    <X extends Throwable>
    FluentFuture<V> catchingAsync(@Nonnull Class<X> exceptionType, @Nonnull Function<? super X, ? extends V> fallback, Executor executor);

    <U> FluentFuture<U> thenHandleAsync(@Nonnull BiFunction<? super V, ? super Throwable, ? extends U> fn, Executor executor);

    FluentFuture<V> whenCompleteAsync(@Nonnull BiConsumer<? super V, ? super Throwable> action, Executor executor);

    FluentFuture<V> whenExceptionallyAsync(@Nonnull Consumer<? super Throwable> action, Executor executor);

}
