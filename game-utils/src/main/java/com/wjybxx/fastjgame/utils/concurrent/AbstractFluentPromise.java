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

import com.wjybxx.fastjgame.utils.ConcurrentUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 实现参考{@link CompletableFuture}
 * 设计该超类是为了分散实现，避免某一个类过于庞大，这里主要借助了{@link FluentFuture#acceptNow(BiConsumer)}接口。
 * <p>
 * 功力有限，为了降低难度和复杂度，实现上进行了一定的简化，缺少{@link CompletableFuture}中许多优化。
 * 不然过于复杂，难以保证正确性，且难以维护。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/4/26
 * github - https://github.com/hl845740757
 */
public abstract class AbstractFluentPromise<V> extends AbstractPromise<V> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractFluentPromise.class);

    public AbstractFluentPromise() {
    }

    public AbstractFluentPromise(V result) {
        super(result);
    }

    public AbstractFluentPromise(Throwable cause) {
        super(cause);
    }

    @Override
    public <U> Promise<U> thenCompose(@Nonnull Function<? super V, ? extends FluentFuture<U>> fn) {
        final Promise<U> promise = newIncompletePromise();
        pushCompletion(new UniComposeApply<>(null, this, promise, fn));
        return promise;
    }

    @Override
    public <U> Promise<U> thenCompose(@Nonnull Callable<? extends FluentFuture<U>> fn) {
        final Promise<U> promise = newIncompletePromise();
        pushCompletion(new UniComposeCall<>(null, this, promise, fn));
        return promise;
    }

    @Override
    public Promise<Void> thenRun(@Nonnull Runnable action) {
        final Promise<Void> promise = newIncompletePromise();
        pushCompletion(new UniRun<>(null, this, promise, action));
        return promise;
    }

    @Override
    public <U> Promise<U> thenCall(@Nonnull Callable<U> fn) {
        final Promise<U> promise = newIncompletePromise();
        pushCompletion(new UniCall<>(null, this, promise, fn));
        return promise;
    }

    @Override
    public Promise<Void> thenAccept(@Nonnull Consumer<? super V> action) {
        final Promise<Void> promise = newIncompletePromise();
        pushCompletion(new UniAccept<>(null, this, promise, action));
        return promise;
    }

    @Override
    public <U> Promise<U> thenApply(@Nonnull Function<? super V, ? extends U> fn) {
        final Promise<U> promise = newIncompletePromise();
        pushCompletion(new UniApply<>(null, this, promise, fn));
        return promise;
    }

    @Override
    public <X extends Throwable> Promise<V> catching(@Nonnull Class<X> exceptionType, @Nonnull Function<? super X, ? extends V> fallback) {
        final Promise<V> promise = newIncompletePromise();
        pushCompletion(new UniCaching<>(null, this, promise, exceptionType, fallback));
        return promise;
    }

    @Override
    public <U> Promise<U> thenHandle(@Nonnull BiFunction<? super V, ? super Throwable, ? extends U> fn) {
        final Promise<U> promise = newIncompletePromise();
        pushCompletion(new UniHandle<>(null, this, promise, fn));
        return promise;
    }

    @Override
    public Promise<V> whenComplete(@Nonnull BiConsumer<? super V, ? super Throwable> action) {
        final Promise<V> promise = newIncompletePromise();
        pushCompletion(new UniWhenComplete<>(null, this, promise, action));
        return promise;
    }

    @Override
    public Promise<V> whenExceptionally(@Nonnull Consumer<? super Throwable> action) {
        final Promise<V> promise = newIncompletePromise();
        pushCompletion(new UniWhenExceptionally<>(null, this, promise, action));
        return promise;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public <U> Promise<U> thenComposeAsync(@Nonnull Function<? super V, ? extends FluentFuture<U>> fn, Executor executor) {
        final Promise<U> promise = newIncompletePromise();
        pushCompletion(new UniComposeApply<>(executor, this, promise, fn));
        return promise;
    }

    @Override
    public <U> Promise<U> thenComposeAsync(@Nonnull Callable<? extends FluentFuture<U>> fn, Executor executor) {
        final Promise<U> promise = newIncompletePromise();
        pushCompletion(new UniComposeCall<>(executor, this, promise, fn));
        return promise;
    }

    @Override
    public Promise<Void> thenRunAsync(@Nonnull Runnable action, Executor executor) {
        final Promise<Void> promise = newIncompletePromise();
        pushCompletion(new UniRun<>(executor, this, promise, action));
        return promise;
    }

    @Override
    public <U> Promise<U> thenCallAsync(@Nonnull Callable<U> fn, Executor executor) {
        final Promise<U> promise = newIncompletePromise();
        pushCompletion(new UniCall<>(executor, this, promise, fn));
        return promise;
    }

    @Override
    public Promise<Void> thenAcceptAsync(@Nonnull Consumer<? super V> action, Executor executor) {
        final Promise<Void> promise = newIncompletePromise();
        pushCompletion(new UniAccept<>(executor, this, promise, action));
        return promise;
    }

    @Override
    public <U> Promise<U> thenApplyAsync(@Nonnull Function<? super V, ? extends U> fn, Executor executor) {
        final Promise<U> promise = newIncompletePromise();
        pushCompletion(new UniApply<>(executor, this, promise, fn));
        return promise;
    }

    @Override
    public <X extends Throwable>
    Promise<V> catchingAsync(@Nonnull Class<X> exceptionType, @Nonnull Function<? super X, ? extends V> fallback, Executor executor) {
        final Promise<V> promise = newIncompletePromise();
        pushCompletion(new UniCaching<>(executor, this, promise, exceptionType, fallback));
        return promise;
    }

    @Override
    public <U> Promise<U> thenHandleAsync(@Nonnull BiFunction<? super V, ? super Throwable, ? extends U> fn, Executor executor) {
        final Promise<U> promise = newIncompletePromise();
        pushCompletion(new UniHandle<>(executor, this, promise, fn));
        return promise;
    }

    @Override
    public Promise<V> whenCompleteAsync(@Nonnull BiConsumer<? super V, ? super Throwable> action, Executor executor) {
        final Promise<V> promise = newIncompletePromise();
        pushCompletion(new UniWhenComplete<>(executor, this, promise, action));
        return promise;
    }

    @Override
    public Promise<V> whenExceptionallyAsync(@Nonnull Consumer<? super Throwable> action, Executor executor) {
        final Promise<V> promise = newIncompletePromise();
        pushCompletion(new UniWhenExceptionally<>(executor, this, promise, action));
        return promise;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public Promise<V> addListener(FutureListener<? super V> listener) {
        return this;
    }

    @Override
    public Promise<V> addListener(FutureListener<? super V> listener, Executor executor) {
        return this;
    }

    @Override
    public ListenableFuture<V> addListener(@Nonnull BiConsumer<? super V, ? super Throwable> action) {
        if (action instanceof Completion) {
            pushCompletion((Completion) action);
        } else {
            pushCompletion(new ListenWhenComplete<>(null, this, action));
        }
        return this;
    }

    @Override
    public ListenableFuture<V> addListener(@Nonnull BiConsumer<? super V, ? super Throwable> action, Executor executor) {
        pushCompletion(new ListenWhenComplete<>(executor, this, action));
        return this;
    }

    @Override
    public Promise<V> addFailedListener(Consumer<? super Throwable> action) {
        pushCompletion(new ListenWhenExceptionally<>(null, this, action));
        return this;
    }

    @Override
    public Promise<V> addFailedListener(Consumer<? super Throwable> action, Executor executor) {
        pushCompletion(new ListenWhenExceptionally<>(executor, this, action));
        return this;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * 创建一个具体的子类型对象，用于作为各个方法的返回值。
     */
    protected abstract <U> Promise<U> newIncompletePromise();

    protected abstract void pushCompletion(Completion completion);

    /**
     * {@link AbstractPromise}的实现类需要在完成的时候，调用{@link #tryFire()}
     */
    protected static abstract class Completion {

        /**
         * 子类实现不应抛出异常
         */
        abstract void tryFire();

    }

    // -------------------------------------------------- UniCompletion ---------------------------------------------------------------

    /**
     * {@link UniCompletion}表示联合两个{@code Future}，因此它持有一个输入，一个动作，和一个输出。
     * 实现{@link Runnable}接口是为了避免创建不必要的对象，如果需要在另一个线程执行的时候。
     *
     * @param <V> 输入值类型
     * @param <U> 输入值类型
     */
    static abstract class UniCompletion<V, U> extends Completion implements Runnable, BiConsumer<V, Throwable> {

        final Executor executor;
        final FluentFuture<V> input;
        final Promise<U> output;

        UniCompletion(Executor executor, FluentFuture<V> input, Promise<U> output) {
            this.executor = executor;
            this.input = input;
            this.output = output;
        }

        @Override
        protected final void tryFire() {
            if (null == executor || EventLoopUtils.inEventLoop(executor)) {
                input.acceptNow(this);
            } else {
                ConcurrentUtils.safeExecute(executor, this);
            }
        }

        @Override
        public final void run() {
            input.acceptNow(this);
        }

        /**
         * 子类实现不应抛出异常
         *
         * @param value input正常完成时的结果
         * @param cause input异常完成时的结果
         */
        @Override
        public abstract void accept(V value, Throwable cause);

    }

    static class UniComposeApply<V, U> extends UniCompletion<V, U> {

        final Function<? super V, ? extends FluentFuture<U>> fn;

        UniComposeApply(Executor executor, FluentFuture<V> input, Promise<U> output,
                        Function<? super V, ? extends FluentFuture<U>> fn) {
            super(executor, input, output);
            this.fn = fn;
        }

        @Override
        public void accept(V value, Throwable cause) {
            if (cause != null) {
                output.tryFailure(cause);
            } else {
                try {
                    final FluentFuture<U> relay = fn.apply(value);
                    relay.addListener(new UniRelay<>(relay, output));
                } catch (Throwable ex) {
                    output.tryFailure(ex);
                }
            }
        }
    }

    static class UniRelay<V> extends UniCompletion<V, V> {

        UniRelay(FluentFuture<V> input, Promise<V> output) {
            super(null, input, output);
        }

        @Override
        public void accept(V value, Throwable cause) {
            if (cause == null) {
                output.trySuccess(value);
            } else {
                output.tryFailure(cause);
            }
        }
    }

    static class UniComposeCall<V, U> extends UniCompletion<V, U> {

        final Callable<? extends FluentFuture<U>> fn;

        UniComposeCall(Executor executor, FluentFuture<V> input, Promise<U> output,
                       Callable<? extends FluentFuture<U>> fn) {
            super(executor, input, output);
            this.fn = fn;
        }

        @Override
        public void accept(V value, Throwable cause) {
            if (cause != null) {
                output.tryFailure(cause);
            } else {
                try {
                    final FluentFuture<U> relay = fn.call();
                    relay.addListener(new UniRelay<>(relay, output));
                } catch (Throwable ex) {
                    output.tryFailure(ex);
                }
            }
        }
    }

    static class UniRun<V> extends UniCompletion<V, Void> {

        final Runnable action;

        UniRun(Executor executor, FluentFuture<V> input, Promise<Void> output,
               Runnable action) {
            super(executor, input, output);
            this.action = action;
        }

        @Override
        public void accept(V value, Throwable cause) {
            if (cause != null) {
                output.tryFailure(cause);
            } else {
                try {
                    action.run();
                    output.trySuccess(null);
                } catch (Throwable ex) {
                    output.tryFailure(ex);
                }
            }
        }

    }

    static class UniCall<V, U> extends UniCompletion<V, U> {

        final Callable<U> fn;

        UniCall(Executor executor, FluentFuture<V> input, Promise<U> output,
                Callable<U> fn) {
            super(executor, input, output);
            this.fn = fn;
        }

        @Override
        public void accept(V value, Throwable cause) {
            if (cause != null) {
                output.tryFailure(cause);
            } else {
                try {
                    final U newValue = fn.call();
                    output.trySuccess(newValue);
                } catch (Throwable ex) {
                    output.tryFailure(ex);
                }
            }
        }

    }

    static class UniAccept<V> extends UniCompletion<V, Void> {

        final Consumer<? super V> action;

        UniAccept(Executor executor, FluentFuture<V> input, Promise<Void> output,
                  Consumer<? super V> action) {
            super(executor, input, output);
            this.action = action;
        }

        @Override
        public void accept(V value, Throwable cause) {
            if (cause != null) {
                output.tryFailure(cause);
            } else {
                try {
                    action.accept(value);
                    output.trySuccess(null);
                } catch (Throwable ex) {
                    output.tryFailure(ex);
                }
            }
        }

    }

    static class UniApply<V, U> extends UniCompletion<V, U> {

        final Function<? super V, ? extends U> fn;

        UniApply(Executor executor, FluentFuture<V> input, Promise<U> output,
                 Function<? super V, ? extends U> fn) {
            super(executor, input, output);
            this.fn = fn;
        }

        @Override
        public void accept(V value, Throwable cause) {
            if (cause != null) {
                output.tryFailure(cause);
            } else {
                try {
                    final U newValue = fn.apply(value);
                    output.trySuccess(newValue);
                } catch (Throwable ex) {
                    output.tryFailure(ex);
                }
            }
        }

    }

    static class UniCaching<V, X> extends UniCompletion<V, V> {

        final Class<X> exceptionType;
        final Function<? super X, ? extends V> fallback;

        UniCaching(Executor executor, FluentFuture<V> input, Promise<V> output,
                   Class<X> exceptionType, Function<? super X, ? extends V> fallback) {
            super(executor, input, output);
            this.exceptionType = exceptionType;
            this.fallback = fallback;
        }

        @Override
        public void accept(V value, Throwable cause) {
            if (cause == null) {
                output.trySuccess(value);
                return;
            }

            if (!exceptionType.isInstance(cause)) {
                output.tryFailure(cause);
                return;
            }

            try {
                @SuppressWarnings("unchecked") final X castException = (X) cause;
                output.trySuccess(fallback.apply(castException));
            } catch (Throwable ex) {
                output.tryFailure(ex);
            }
        }

    }

    static class UniHandle<V, U> extends UniCompletion<V, U> {

        final BiFunction<? super V, ? super Throwable, ? extends U> fn;

        UniHandle(Executor executor, FluentFuture<V> input, Promise<U> output,
                  BiFunction<? super V, ? super Throwable, ? extends U> fn) {
            super(executor, input, output);
            this.fn = fn;
        }

        @Override
        public void accept(V value, Throwable cause) {
            try {
                output.trySuccess(fn.apply(value, cause));
            } catch (Throwable ex) {
                output.tryFailure(ex);
            }
        }

    }

    static class UniWhenComplete<V> extends UniCompletion<V, V> {

        final BiConsumer<? super V, ? super Throwable> action;

        UniWhenComplete(Executor executor, FluentFuture<V> input, Promise<V> output,
                        BiConsumer<? super V, ? super Throwable> action) {
            super(executor, input, output);
            this.action = action;
        }

        @Override
        public void accept(V value, Throwable cause) {
            try {
                action.accept(value, cause);
            } catch (Throwable ex) {
                // 这里的实现与JDK不同，这里仅仅是记录一个异常，不会传递给下一个Future
                logger.warn("UniWhenComplete.action.accept caught exception", ex);
            }

            if (cause != null) {
                output.tryFailure(cause);
            } else {
                output.trySuccess(value);
            }
        }

    }

    static class UniWhenExceptionally<V> extends UniCompletion<V, V> {

        final Consumer<? super Throwable> action;

        UniWhenExceptionally(Executor executor, FluentFuture<V> input, Promise<V> output,
                             Consumer<? super Throwable> action) {
            super(executor, input, output);
            this.action = action;
        }

        @Override
        public void accept(V value, Throwable cause) {
            if (cause == null) {
                output.trySuccess(value);
                return;
            }

            try {
                action.accept(cause);
            } catch (Throwable ex) {
                // 这里仅仅是记录一个异常，不会传递给下一个Future
                logger.warn("UniWhenExceptionally.action.accept caught exception", ex);
            }

            output.tryFailure(cause);
        }

    }

    static abstract class ListenCompletion<V> extends Completion implements Runnable, BiConsumer<V, Throwable> {

        final Executor executor;
        final FluentFuture<V> input;

        ListenCompletion(Executor executor, FluentFuture<V> input) {
            this.executor = executor;
            this.input = input;
        }

        @Override
        public final void run() {
            input.acceptNow(this);
        }

        @Override
        void tryFire() {
            if (executor == null || EventLoopUtils.inEventLoop(executor)) {
                input.acceptNow(this);
            } else {
                ConcurrentUtils.safeExecute(executor, this);
            }
        }

        @Override
        public abstract void accept(V value, Throwable cause);

    }

    static class ListenWhenComplete<V> extends ListenCompletion<V> {

        final BiConsumer<? super V, ? super Throwable> action;

        public ListenWhenComplete(Executor executor, FluentFuture<V> input,
                                  BiConsumer<? super V, ? super Throwable> action) {
            super(executor, input);
            this.action = action;
        }

        @Override
        public void accept(V value, Throwable cause) {
            try {
                action.accept(value, cause);
            } catch (Throwable ex) {
                // 这里的实现与JDK不同，这里仅仅是记录一个异常，不会传递给下一个Future
                logger.warn("ListenWhenComplete.action.accept caught exception", ex);
            }
        }
    }

    static class ListenWhenExceptionally<V> extends ListenCompletion<V> {

        final Consumer<? super Throwable> action;

        public ListenWhenExceptionally(Executor executor, FluentFuture<V> input,
                                       Consumer<? super Throwable> action) {
            super(executor, input);
            this.action = action;
        }

        @Override
        public void accept(V value, Throwable cause) {
            if (cause == null) {
                return;
            }

            try {
                action.accept(cause);
            } catch (Throwable ex) {
                // 这里仅仅是记录一个异常，不会传递给下一个Future
                logger.warn("ListenWhenExceptionally.action.accept caught exception", ex);
            }
        }
    }

}
