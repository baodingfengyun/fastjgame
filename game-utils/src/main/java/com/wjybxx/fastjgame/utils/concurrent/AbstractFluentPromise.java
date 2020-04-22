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

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 实现参考{@link java.util.concurrent.CompletableFuture}
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/4/22
 */
public abstract class AbstractFluentPromise<V> extends AbstractPromise<V> implements FluentPromise<V> {

    @Override
    public <U> FluentFuture<U> thenCompose(Function<? super V, ? extends ListenableFuture<U>> fn) {
        final AbstractFluentPromise<U> promise = newIncompletePromise();
        addListener(new UniCompose<>(this, promise, fn));
        return promise;
    }

    @Override
    public FluentFuture<Void> thenRun(Runnable action) {
        final AbstractFluentPromise<Void> promise = newIncompletePromise();
        addListener(new UniRun<>(this, promise, action));
        return promise;
    }

    @Override
    public FluentFuture<Void> thenAccept(Consumer<? super V> action) {
        final AbstractFluentPromise<Void> promise = newIncompletePromise();
        addListener(new UniAccept<>(this, promise, action));
        return promise;
    }

    @Override
    public <U> FluentFuture<U> thenApply(Function<? super V, ? extends U> fn) {
        final AbstractFluentPromise<U> promise = newIncompletePromise();
        addListener(new UniApply<>(this, promise, fn));
        return promise;
    }

    @Override
    public FluentFuture<V> whenComplete(BiConsumer<? super V, ? super Throwable> action) {
        final AbstractFluentPromise<V> promise = newIncompletePromise();
        addListener(new UniComplete<>(this, promise, action));
        return promise;
    }

    @Override
    public <U> FluentFuture<U> thenHandle(BiFunction<? super V, ? super Throwable, ? extends U> fn) {
        final AbstractFluentPromise<U> promise = newIncompletePromise();
        addListener(new UniHandle<>(this, promise, fn));
        return promise;
    }

    @Override
    public FluentFuture<V> exceptionally(Function<Throwable, ? extends V> fn) {
        final FluentPromise<V> promise = newIncompletePromise();
        addListener(new UniExceptionally<>(this, promise, fn));
        return promise;
    }

    /**
     * 创建一个具体的子类型对象，用于作为各个方法的返回值。
     */
    protected abstract <U> AbstractFluentPromise<U> newIncompletePromise();

    /**
     * 实现{@link Runnable}接口是为了避免创建不必要的对象
     */
    static abstract class UniCompletion<V, U> implements Runnable, FutureListener<V> {

        final ListenableFuture<V> input;
        final Promise<U> output;

        UniCompletion(ListenableFuture<V> input, Promise<U> output) {
            this.input = input;
            this.output = output;
        }

        @Override
        public final void run() {
            onComplete(input);
        }

        /**
         * 子类实现不可抛出异常
         */
        @Override
        public abstract void onComplete(ListenableFuture<V> future);

    }

    static class UniCompose<V, U> extends UniCompletion<V, U> {

        final Function<? super V, ? extends ListenableFuture<U>> fn;

        UniCompose(ListenableFuture<V> input, Promise<U> output,
                   Function<? super V, ? extends ListenableFuture<U>> fn) {
            super(input, output);
            this.fn = fn;
        }

        @Override
        public void onComplete(ListenableFuture<V> future) {
            if (future.isCompletedExceptionally()) {
                output.tryFailure(future.cause());
            } else {
                try {
                    final ListenableFuture<U> relay = fn.apply(future.getNow());
                    relay.addListener(new UniRelay<>(relay, output));
                } catch (Throwable ex) {
                    output.tryFailure(ex);
                }
            }
        }
    }

    static class UniRelay<V> extends UniCompletion<V, V> {

        UniRelay(ListenableFuture<V> input, Promise<V> output) {
            super(input, output);
        }

        @Override
        public void onComplete(ListenableFuture<V> future) {
            if (future.isCompletedExceptionally()) {
                output.tryFailure(future.cause());
            } else {
                output.trySuccess(future.getNow());
            }
        }
    }

    static class UniRun<V> extends UniCompletion<V, Void> {

        final Runnable action;

        UniRun(ListenableFuture<V> input, Promise<Void> output, Runnable action) {
            super(input, output);
            this.action = action;
        }

        @Override
        public void onComplete(ListenableFuture<V> future) {
            if (future.isCompletedExceptionally()) {
                output.tryFailure(future.cause());
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

    static class UniAccept<V> extends UniCompletion<V, Void> {

        final Consumer<? super V> action;

        UniAccept(ListenableFuture<V> input, Promise<Void> output, Consumer<? super V> action) {
            super(input, output);
            this.action = action;
        }

        @Override
        public void onComplete(ListenableFuture<V> future) {
            if (future.isCompletedExceptionally()) {
                output.tryFailure(future.cause());
            } else {
                try {
                    action.accept(future.getNow());
                    output.trySuccess(null);
                } catch (Throwable ex) {
                    output.tryFailure(ex);
                }
            }
        }
    }

    static class UniApply<V, U> extends UniCompletion<V, U> {

        final Function<? super V, ? extends U> fn;

        UniApply(ListenableFuture<V> input, Promise<U> output,
                 Function<? super V, ? extends U> fn) {
            super(input, output);
            this.fn = fn;
        }

        @Override
        public void onComplete(ListenableFuture<V> future) {
            if (future.isCompletedExceptionally()) {
                output.tryFailure(future.cause());
            } else {
                try {
                    final U newValue = fn.apply(future.getNow());
                    output.trySuccess(newValue);
                } catch (Throwable ex) {
                    output.tryFailure(ex);
                }
            }
        }
    }

    static class UniComplete<V> extends UniCompletion<V, V> {

        final BiConsumer<? super V, ? super Throwable> action;

        UniComplete(ListenableFuture<V> input, Promise<V> output,
                    BiConsumer<? super V, ? super Throwable> action) {
            super(input, output);
            this.action = action;
        }

        @Override
        public void onComplete(ListenableFuture<V> future) {
            final V value;
            Throwable cause;
            if (future.isCompletedExceptionally()) {
                value = null;
                cause = future.cause();
            } else {
                value = future.getNow();
                cause = null;
            }

            try {
                action.accept(value, cause);
            } catch (Throwable ex) {
                if (cause == null) {
                    cause = ex;
                } else {
                    cause.addSuppressed(ex);
                }
            }

            if (cause == null) {
                output.trySuccess(value);
            } else {
                output.tryFailure(cause);
            }
        }
    }

    static class UniHandle<V, U> extends UniCompletion<V, U> {

        final BiFunction<? super V, ? super Throwable, ? extends U> fn;

        UniHandle(ListenableFuture<V> input, Promise<U> output,
                  BiFunction<? super V, ? super Throwable, ? extends U> fn) {
            super(input, output);
            this.fn = fn;
        }

        @Override
        public void onComplete(ListenableFuture<V> future) {
            final V value;
            final Throwable cause;

            if (future.isCompletedExceptionally()) {
                value = null;
                cause = future.cause();
            } else {
                value = future.getNow();
                cause = null;
            }

            try {
                output.trySuccess(fn.apply(value, cause));
            } catch (Throwable ex) {
                output.tryFailure(ex);
            }
        }
    }

    static class UniExceptionally<V> extends UniCompletion<V, V> {

        final Function<Throwable, ? extends V> fn;

        UniExceptionally(ListenableFuture<V> input, Promise<V> output,
                         Function<Throwable, ? extends V> fn) {
            super(input, output);
            this.fn = fn;
        }

        @Override
        public void onComplete(ListenableFuture<V> future) {
            if (future.isCompletedExceptionally()) {
                try {
                    output.trySuccess(fn.apply(future.cause()));
                } catch (Throwable ex) {
                    output.tryFailure(ex);
                }
            } else {
                output.trySuccess(future.getNow());
            }
        }
    }

}
