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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 实现参考{@link java.util.concurrent.CompletableFuture}
 * 设计该超类是为了分散实现，避免某一个类过于庞大，这里主要借助了{@link FluentFuture#acceptNow(BiConsumer)}接口。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/4/26
 * github - https://github.com/hl845740757
 */
public abstract class BasePromise<V> implements Promise<V> {

    private static final Logger logger = LoggerFactory.getLogger(BasePromise.class);

    @Override
    public <U> Promise<U> thenCompose(@Nonnull Function<? super V, ? extends FluentFuture<U>> fn) {
        final Promise<U> promise = newIncompletePromise();
        pushCompletion(new UniCompose<>(this, promise, fn));
        return promise;
    }

    @Override
    public <U> Promise<U> thenCompose(@Nonnull Callable<? extends FluentFuture<U>> fn) {
        final Promise<U> promise = newIncompletePromise();
        pushCompletion(new UniCompose2<>(this, promise, fn));
        return promise;
    }

    @Override
    public Promise<Void> thenRun(@Nonnull Runnable action) {
        final Promise<Void> promise = newIncompletePromise();
        pushCompletion(new UniRun<>(this, promise, action));
        return promise;
    }

    @Override
    public <U> Promise<U> thenCall(@Nonnull Callable<U> fn) {
        final Promise<U> promise = newIncompletePromise();
        pushCompletion(new UniCall<>(this, promise, fn));
        return promise;
    }

    @Override
    public Promise<Void> thenAccept(@Nonnull Consumer<? super V> action) {
        final Promise<Void> promise = newIncompletePromise();
        pushCompletion(new UniAccept<>(this, promise, action));
        return promise;
    }

    @Override
    public <U> Promise<U> thenApply(@Nonnull Function<? super V, ? extends U> fn) {
        final Promise<U> promise = newIncompletePromise();
        pushCompletion(new UniApply<>(this, promise, fn));
        return promise;
    }

    @Override
    public <X extends Throwable> Promise<V> catching(@Nonnull Class<X> exceptionType, @Nonnull Function<? super X, ? extends V> fallback) {
        final Promise<V> promise = newIncompletePromise();
        pushCompletion(new UniCaching<>(this, promise, exceptionType, fallback));
        return promise;
    }

    @Override
    public <U> Promise<U> thenHandle(@Nonnull BiFunction<? super V, ? super Throwable, ? extends U> fn) {
        final Promise<U> promise = newIncompletePromise();
        pushCompletion(new UniHandle<>(this, promise, fn));
        return promise;
    }

    @Override
    public Promise<V> whenComplete(@Nonnull BiConsumer<? super V, ? super Throwable> action) {
        final Promise<V> promise = newIncompletePromise();
        pushCompletion(new UniWhenComplete<>(this, promise, action));
        return promise;
    }

    @Override
    public Promise<V> whenExceptionally(@Nonnull Consumer<? super Throwable> action) {
        final Promise<V> promise = newIncompletePromise();
        pushCompletion(new UniWhenExceptionally<>(this, promise, action));
        return promise;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public <U> Promise<U> thenComposeAsync(@Nonnull Function<? super V, ? extends FluentFuture<U>> fn, Executor executor) {
        return null;
    }

    @Override
    public <U> Promise<U> thenComposeAsync(@Nonnull Callable<? extends FluentFuture<U>> fn, Executor executor) {
        return null;
    }

    @Override
    public Promise<Void> thenRunAsync(@Nonnull Runnable action, Executor executor) {
        return null;
    }

    @Override
    public <U> Promise<U> thenCallAsync(@Nonnull Callable<U> fn, Executor executor) {
        return null;
    }

    @Override
    public Promise<Void> thenAcceptAsync(@Nonnull Consumer<? super V> action, Executor executor) {
        return null;
    }

    @Override
    public <U> Promise<U> thenApplyAsync(@Nonnull Function<? super V, ? extends U> fn, Executor executor) {
        return null;
    }

    @Override
    public <X extends Throwable> Promise<V> catchingAsync(@Nonnull Class<X> exceptionType, @Nonnull Function<? super X, ? extends V> fallback, Executor executor) {
        return null;
    }

    @Override
    public <U> Promise<U> thenHandleAsync(@Nonnull BiFunction<? super V, ? super Throwable, ? extends U> fn, Executor executor) {
        return null;
    }

    @Override
    public Promise<V> whenCompleteAsync(@Nonnull BiConsumer<? super V, ? super Throwable> action, Executor executor) {
        return null;
    }

    @Override
    public Promise<V> whenExceptionallyAsync(@Nonnull Consumer<? super Throwable> action, Executor executor) {
        return null;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void addListener(FutureListener<? super V> listener) {

    }

    @Override
    public void addListener(FutureListener<? super V> listener, Executor executor) {

    }

    ///////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * 创建一个具体的子类型对象，用于作为各个方法的返回值。
     */
    protected abstract <U> Promise<U> newIncompletePromise();

    /**
     * 子类需要保证：先压入的先通知，后压入的后通知。
     */
    protected abstract void pushCompletion(Completion completion);

    /**
     * {@link AbstractPromise}的实现类需要在完成的时候，调用{@link Completion#run()}方法或{@link #onComplete()}
     * 实现{@link Runnable}接口是为了避免创建不必要的对象，如果需要在另一个线程执行的时候。
     */
    protected static abstract class Completion implements Runnable {

        @Override
        public final void run() {
            onComplete();
        }

        /**
         * 子类实现不应抛出任何异常
         */
        protected abstract void onComplete();

    }

    // -------------------------------------------------- UniCompletion ---------------------------------------------------------------

    /**
     * {@link UniCompletion}表示联合两个{@code Future}
     * 因此它持有一个输入，一个动作，和一个输出。
     *
     * @param <V> 输入值类型
     * @param <U> 输入值类型
     */
    static abstract class UniCompletion<V, U> extends Completion implements BiConsumer<V, Throwable> {

        final FluentFuture<V> input;
        final Promise<U> output;

        UniCompletion(FluentFuture<V> input, Promise<U> output) {
            this.input = input;
            this.output = output;
        }

        @Override
        protected final void onComplete() {
            input.acceptNow(this);
        }

        @Override
        public abstract void accept(V value, Throwable cause);

    }

    static class UniCompose<V, U> extends UniCompletion<V, U> {

        final Function<? super V, ? extends FluentFuture<U>> fn;

        UniCompose(FluentFuture<V> input, Promise<U> output,
                   Function<? super V, ? extends FluentFuture<U>> fn) {
            super(input, output);
            this.fn = fn;
        }

        @Override
        public void accept(V value, Throwable cause) {
            if (cause != null) {
                output.tryFailure(cause);
            } else {
                try {
                    final FluentFuture<U> relay = fn.apply(value);
                    relay.whenComplete(new UniRelay<>(relay, output));
                } catch (Throwable ex) {
                    output.tryFailure(ex);
                }
            }
        }
    }

    static class UniRelay<V> extends UniCompletion<V, V> {

        UniRelay(FluentFuture<V> input, Promise<V> output) {
            super(input, output);
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

    static class UniCompose2<V, U> extends UniCompletion<V, U> {

        final Callable<? extends FluentFuture<U>> fn;

        UniCompose2(FluentFuture<V> input, Promise<U> output,
                    Callable<? extends FluentFuture<U>> fn) {
            super(input, output);
            this.fn = fn;
        }

        @Override
        public void accept(V value, Throwable cause) {
            if (cause != null) {
                output.tryFailure(cause);
            } else {
                try {
                    final FluentFuture<U> relay = fn.call();
                    relay.whenComplete(new UniRelay<>(relay, output));
                } catch (Throwable ex) {
                    output.tryFailure(ex);
                }
            }
        }
    }

    static class UniRun<V> extends UniCompletion<V, Void> {

        final Runnable action;

        UniRun(FluentFuture<V> input, Promise<Void> output, Runnable action) {
            super(input, output);
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

        UniCall(FluentFuture<V> input, Promise<U> output, Callable<U> fn) {
            super(input, output);
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

        UniAccept(FluentFuture<V> input, Promise<Void> output, Consumer<? super V> action) {
            super(input, output);
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

        UniApply(FluentFuture<V> input, Promise<U> output,
                 Function<? super V, ? extends U> fn) {
            super(input, output);
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

        UniCaching(FluentFuture<V> input, Promise<V> output,
                   Class<X> exceptionType, Function<? super X, ? extends V> fallback) {
            super(input, output);
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

        UniHandle(FluentFuture<V> input, Promise<U> output,
                  BiFunction<? super V, ? super Throwable, ? extends U> fn) {
            super(input, output);
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

        UniWhenComplete(FluentFuture<V> input, Promise<V> output,
                        BiConsumer<? super V, ? super Throwable> action) {
            super(input, output);
            this.action = action;
        }

        @Override
        public void accept(V value, Throwable cause) {
            try {
                action.accept(value, cause);
            } catch (Throwable ex) {
                // 这里的实现与JDK不同，这里仅仅是记录一个异常，而不会影响另一个监听器
                logger.warn("UniWhenComplete.action.accept caught exception", ex);
            }
        }

    }

    static class UniWhenExceptionally<V> extends UniCompletion<V, V> {

        final Consumer<? super Throwable> action;

        UniWhenExceptionally(FluentFuture<V> input, Promise<V> output,
                             Consumer<? super Throwable> action) {
            super(input, output);
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
                logger.warn("UniWhenExceptionally.action.accept caught exception", ex);
            }
        }
    }

}
