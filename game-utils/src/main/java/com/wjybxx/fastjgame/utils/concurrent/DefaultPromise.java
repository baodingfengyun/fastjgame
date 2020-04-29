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

import org.apache.commons.lang3.exception.ExceptionUtils;
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
public class DefaultPromise<V> extends AbstractPromise<V> {

    private static final Logger logger = LoggerFactory.getLogger(DefaultPromise.class);

    public DefaultPromise() {
    }

    public DefaultPromise(V result) {
        super(result);
    }

    public DefaultPromise(Throwable cause) {
        super(cause);
    }

    @Override
    public <U> Promise<U> thenCompose(@Nonnull Function<? super V, ? extends ListenableFuture<U>> fn) {
        final AbstractPromise<U> promise = newIncompletePromise();
        pushCompletion(new UniComposeApply<>(null, this, promise, fn));
        return promise;
    }

    @Override
    public <U> Promise<U> thenCompose(@Nonnull Callable<? extends ListenableFuture<U>> fn) {
        final AbstractPromise<U> promise = newIncompletePromise();
        pushCompletion(new UniComposeCall<>(null, this, promise, fn));
        return promise;
    }

    @Override
    public Promise<Void> thenRun(@Nonnull Runnable action) {
        final AbstractPromise<Void> promise = newIncompletePromise();
        pushCompletion(new UniRun<>(null, this, promise, action));
        return promise;
    }

    @Override
    public <U> Promise<U> thenCall(@Nonnull Callable<U> fn) {
        final AbstractPromise<U> promise = newIncompletePromise();
        pushCompletion(new UniCall<>(null, this, promise, fn));
        return promise;
    }

    @Override
    public Promise<Void> thenAccept(@Nonnull Consumer<? super V> action) {
        final AbstractPromise<Void> promise = newIncompletePromise();
        pushCompletion(new UniAccept<>(null, this, promise, action));
        return promise;
    }

    @Override
    public <U> Promise<U> thenApply(@Nonnull Function<? super V, ? extends U> fn) {
        final AbstractPromise<U> promise = newIncompletePromise();
        pushCompletion(new UniApply<>(null, this, promise, fn));
        return promise;
    }

    @Override
    public <X extends Throwable> Promise<V> catching(@Nonnull Class<X> exceptionType, @Nonnull Function<? super X, ? extends V> fallback) {
        final AbstractPromise<V> promise = newIncompletePromise();
        pushCompletion(new UniCaching<>(null, this, promise, exceptionType, fallback));
        return promise;
    }

    @Override
    public <U> Promise<U> thenHandle(@Nonnull BiFunction<? super V, ? super Throwable, ? extends U> fn) {
        final AbstractPromise<U> promise = newIncompletePromise();
        pushCompletion(new UniHandle<>(null, this, promise, fn));
        return promise;
    }

    @Override
    public Promise<V> whenComplete(@Nonnull BiConsumer<? super V, ? super Throwable> action) {
        final AbstractPromise<V> promise = newIncompletePromise();
        pushCompletion(new UniWhenComplete<>(null, this, promise, action));
        return promise;
    }

    @Override
    public Promise<V> whenExceptionally(@Nonnull Consumer<? super Throwable> action) {
        final AbstractPromise<V> promise = newIncompletePromise();
        pushCompletion(new UniWhenExceptionally<>(null, this, promise, action));
        return promise;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public <U> Promise<U> thenComposeAsync(@Nonnull Function<? super V, ? extends ListenableFuture<U>> fn, Executor executor) {
        final AbstractPromise<U> promise = newIncompletePromise();
        pushCompletion(new UniComposeApply<>(executor, this, promise, fn));
        return promise;
    }

    @Override
    public <U> Promise<U> thenComposeAsync(@Nonnull Callable<? extends ListenableFuture<U>> fn, Executor executor) {
        final AbstractPromise<U> promise = newIncompletePromise();
        pushCompletion(new UniComposeCall<>(executor, this, promise, fn));
        return promise;
    }

    @Override
    public Promise<Void> thenRunAsync(@Nonnull Runnable action, Executor executor) {
        final AbstractPromise<Void> promise = newIncompletePromise();
        pushCompletion(new UniRun<>(executor, this, promise, action));
        return promise;
    }

    @Override
    public <U> Promise<U> thenCallAsync(@Nonnull Callable<U> fn, Executor executor) {
        final AbstractPromise<U> promise = newIncompletePromise();
        pushCompletion(new UniCall<>(executor, this, promise, fn));
        return promise;
    }

    @Override
    public Promise<Void> thenAcceptAsync(@Nonnull Consumer<? super V> action, Executor executor) {
        final AbstractPromise<Void> promise = newIncompletePromise();
        pushCompletion(new UniAccept<>(executor, this, promise, action));
        return promise;
    }

    @Override
    public <U> Promise<U> thenApplyAsync(@Nonnull Function<? super V, ? extends U> fn, Executor executor) {
        final AbstractPromise<U> promise = newIncompletePromise();
        pushCompletion(new UniApply<>(executor, this, promise, fn));
        return promise;
    }

    @Override
    public <X extends Throwable>
    Promise<V> catchingAsync(@Nonnull Class<X> exceptionType, @Nonnull Function<? super X, ? extends V> fallback, Executor executor) {
        final AbstractPromise<V> promise = newIncompletePromise();
        pushCompletion(new UniCaching<>(executor, this, promise, exceptionType, fallback));
        return promise;
    }

    @Override
    public <U> Promise<U> thenHandleAsync(@Nonnull BiFunction<? super V, ? super Throwable, ? extends U> fn, Executor executor) {
        final AbstractPromise<U> promise = newIncompletePromise();
        pushCompletion(new UniHandle<>(executor, this, promise, fn));
        return promise;
    }

    @Override
    public Promise<V> whenCompleteAsync(@Nonnull BiConsumer<? super V, ? super Throwable> action, Executor executor) {
        final AbstractPromise<V> promise = newIncompletePromise();
        pushCompletion(new UniWhenComplete<>(executor, this, promise, action));
        return promise;
    }

    @Override
    public Promise<V> whenExceptionallyAsync(@Nonnull Consumer<? super Throwable> action, Executor executor) {
        final AbstractPromise<V> promise = newIncompletePromise();
        pushCompletion(new UniWhenExceptionally<>(executor, this, promise, action));
        return promise;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////

    @SuppressWarnings("unchecked")
    @Override
    public Promise<V> addListener(FutureListener<? super V> listener) {
        addListener((v, cause) -> {
            try {
                listener.onComplete((ListenableFuture) this);
            } catch (Throwable e) {
                ExceptionUtils.rethrow(e);
            }
        });
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Promise<V> addListener(FutureListener<? super V> listener, Executor executor) {
        addListener((v, cause) -> {
            try {
                listener.onComplete((ListenableFuture) this);
            } catch (Throwable e) {
                ExceptionUtils.rethrow(e);
            }
        }, executor);
        return this;
    }

    @Override
    public Promise<V> addListener(@Nonnull BiConsumer<? super V, ? super Throwable> action) {
        if (action instanceof Completion) {
            pushCompletion((Completion) action);
        } else {
            pushCompletion(new ListenWhenComplete<>(null, this, action));
        }
        return this;
    }

    @Override
    public Promise<V> addListener(@Nonnull BiConsumer<? super V, ? super Throwable> action, Executor executor) {
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

    @Override
    public ListenableFuture<V> addListener(@Nonnull Runnable action) {
        if (action instanceof Completion) {
            pushCompletion((Completion) action);
        }

        return this;
    }

    @Override
    public ListenableFuture<V> addListener(@Nonnull Runnable action, Executor executor) {
        return this;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * 创建一个具体的子类型对象，用于作为各个方法的返回值。
     */
    protected <U> AbstractPromise<U> newIncompletePromise() {
        return new DefaultPromise<>();
    }


    // -------------------------------------------------- UniCompletion ---------------------------------------------------------------

    private static <U> AbstractPromise<U> postFire(@Nonnull AbstractPromise<U> output, int mode) {
        if (isNestedMode(mode)) {
            return output;
        } else {
            postComplete(output);
            return null;
        }
    }

    /**
     * {@link UniCompletion}表示联合两个{@code Future}，因此它持有一个输入，一个动作，和一个输出。
     * 实现{@link Runnable}接口是为了避免创建不必要的对象，如果需要在另一个线程执行的时候。
     */
    static abstract class UniCompletion extends Completion {

        Executor executor;

        UniCompletion(Executor executor) {
            this.executor = executor;
        }

        /**
         * 如果可以立即执行，则返回true，否则将提交到{@link #executor}稍后执行。
         */
        final boolean executeDirectly() {
            Executor e = executor;
            if (e == null || EventLoopUtils.inEventLoop(e)) {
                executor = null;
                return true;
            }

            e.execute(this);
            // help gc
            executor = null;
            return false;
        }
    }

    /**
     * @param <V> 输入值类型
     * @param <U> 输入值类型
     */
    static abstract class AbstractUniCompletion<V, U> extends UniCompletion {

        AbstractPromise<V> input;
        AbstractPromise<U> output;

        AbstractUniCompletion(Executor executor, AbstractPromise<V> input, AbstractPromise<U> output) {
            super(executor);
            this.input = input;
            this.output = output;
        }

    }

    static class UniComposeApply<V, U> extends AbstractUniCompletion<V, U> {

        Function<? super V, ? extends ListenableFuture<U>> fn;

        UniComposeApply(Executor executor, AbstractPromise<V> input, AbstractPromise<U> output,
                        Function<? super V, ? extends ListenableFuture<U>> fn) {
            super(executor, input, output);
            this.fn = fn;
        }

        /**
         * {@link #input}执行成功的情况下才执行
         */
        @Override
        AbstractPromise<?> tryFire(int mode) {
            AbstractPromise<U> out = output;

            // 一直以为循环才能带标签...
            tryComplete:
            if (out.isCancellable()) {
                AbstractPromise<V> in = input;
                Object inResult = in.resultHolder;

                if (inResult instanceof AltResult) {
                    // 上一步异常完成，不执行给定动作，直接完成(当前completion只是简单中继)
                    out.completeRelayThrowable(inResult);
                    break tryComplete;
                }

                try {
                    if (isSyncOrNestedMode(mode) && !executeDirectly()) {
                        return null;
                    }

                    if (!out.setUncancellable()) {
                        break tryComplete;
                    }

                    V value = in.decodeValue(inResult);
                    ListenableFuture<U> relay = fn.apply(value);

                    if (relay.isDone()) {
                        // 返回的是已完成的Future
                        completeRelay(out, relay);
                    } else {
                        relay.addListener(new UniRelay<>(relay, out));
                    }
                } catch (Throwable ex) {
                    out.completeThrowable(ex);
                }
            }

            // 走到这里表示，表示该completion已完成，释放内存
            // help gc
            input = null;
            output = null;
            fn = null;

            return postFire(out, mode);
        }
    }

    private static <U> boolean completeRelay(AbstractPromise<U> out, ListenableFuture<U> relay) {
        if (relay instanceof AbstractPromise) {
            Object result = ((AbstractPromise<U>) relay).resultHolder;
            return out.completeRelay(result);
        }

        Throwable cause = relay.cause();
        if (cause != null) {
            return out.completeThrowable(cause);
        } else {
            return out.completeValue(relay.getNow());
        }
    }

    static class UniRelay<V> extends UniCompletion {

        ListenableFuture<V> input;
        AbstractPromise<V> output;

        UniRelay(ListenableFuture<V> input, AbstractPromise<V> output) {
            super(null);
            this.input = input;
            this.output = output;
        }

        @Override
        AbstractPromise<?> tryFire(int mode) {
            AbstractPromise<V> out = this.output;
            ListenableFuture<V> in = this.input;

            if (out.setUncancellable()) {
                completeRelay(out, in);
            }

            // help gc
            output = null;
            input = null;

            return postFire(out, mode);
        }
    }

    static class UniComposeCall<V, U> extends AbstractUniCompletion<V, U> {

        Callable<? extends ListenableFuture<U>> fn;

        UniComposeCall(Executor executor, AbstractPromise<V> input, AbstractPromise<U> output,
                       Callable<? extends ListenableFuture<U>> fn) {
            super(executor, input, output);
            this.fn = fn;
        }

        @Override
        AbstractPromise<?> tryFire(int mode) {
            AbstractPromise<U> out = output;

            // 一直以为循环才能带标签...主要用于减少嵌套
            tryComplete:
            if (!isDone0(out.resultHolder)) {
                Object inResult = input.resultHolder;

                if (inResult instanceof AltResult) {
                    // 上一步异常完成，不执行给定动作，直接完成
                    out.completeRelayThrowable(inResult);
                    break tryComplete;
                }

                try {
                    if (isSyncOrNestedMode(mode) && !executeDirectly()) {
                        return null;
                    }

                    if (!out.setUncancellable()) {
                        break tryComplete;
                    }

                    ListenableFuture<U> relay = fn.call();

                    if (relay.isDone()) {
                        // 返回的是已完成的Future
                        completeRelay(out, relay);
                    } else {
                        relay.addListener(new UniRelay<>(relay, out));
                    }
                } catch (Throwable ex) {
                    out.completeThrowable(ex);
                }
            }

            // help gc
            input = null;
            output = null;
            fn = null;

            return postFire(out, mode);
        }

    }

    static class UniRun<V> extends AbstractUniCompletion<V, Void> {

        Runnable action;

        UniRun(Executor executor, AbstractPromise<V> input, AbstractPromise<Void> output,
               Runnable action) {
            super(executor, input, output);
            this.action = action;
        }

        @Override
        AbstractPromise<?> tryFire(int mode) {
            AbstractPromise<Void> out = output;

            if (!isDone0(out.resultHolder)) {
                Object inResult = input.resultHolder;

                if (inResult instanceof AltResult) {
                    out.completeRelayThrowable(inResult);
                } else {
                    try {
                        if (isSyncOrNestedMode(mode) && !executeDirectly()) {
                            return null;
                        }

                        if (out.setUncancellable()) {
                            action.run();
                            out.completeNull();
                        }
                    } catch (Throwable ex) {
                        out.completeThrowable(ex);
                    }
                }
            }

            input = null;
            output = null;
            action = null;

            return postFire(out, mode);
        }

    }

    static class UniCall<V, U> extends AbstractUniCompletion<V, U> {

        Callable<U> fn;

        UniCall(Executor executor, AbstractPromise<V> input, AbstractPromise<U> output,
                Callable<U> fn) {
            super(executor, input, output);
            this.fn = fn;
        }

        @Override
        AbstractPromise<?> tryFire(int mode) {
            AbstractPromise<U> out = output;

            if (!isDone0(out.resultHolder)) {
                Object inResult = input.resultHolder;

                if (inResult instanceof AltResult) {
                    out.completeRelayThrowable(inResult);
                } else {
                    try {
                        if (isSyncOrNestedMode(mode) && !executeDirectly()) {
                            return null;
                        }

                        if (out.setUncancellable()) {
                            out.completeValue(fn.call());
                        }
                    } catch (Throwable ex) {
                        out.completeThrowable(ex);
                    }
                }
            }

            input = null;
            output = null;
            fn = null;

            return postFire(out, mode);
        }

    }

    static class UniAccept<V> extends AbstractUniCompletion<V, Void> {

        Consumer<? super V> action;

        UniAccept(Executor executor, AbstractPromise<V> input, AbstractPromise<Void> output,
                  Consumer<? super V> action) {
            super(executor, input, output);
            this.action = action;
        }

        @Override
        AbstractPromise<?> tryFire(int mode) {
            AbstractPromise<Void> out = output;

            if (!isDone0(out.resultHolder)) {
                AbstractPromise<V> in = input;
                Object inResult = in.resultHolder;

                if (inResult instanceof AltResult) {
                    out.completeRelayThrowable(inResult);
                } else {
                    try {
                        if (isSyncOrNestedMode(mode) && !executeDirectly()) {
                            return null;
                        }

                        if (out.setUncancellable()) {
                            V value = in.decodeValue(inResult);
                            action.accept(value);
                            out.completeNull();
                        }

                    } catch (Throwable ex) {
                        out.completeThrowable(ex);
                    }
                }
            }

            input = null;
            output = null;
            action = null;

            return postFire(out, mode);
        }

    }

    static class UniApply<V, U> extends AbstractUniCompletion<V, U> {

        Function<? super V, ? extends U> fn;

        UniApply(Executor executor, AbstractPromise<V> input, AbstractPromise<U> output,
                 Function<? super V, ? extends U> fn) {
            super(executor, input, output);
            this.fn = fn;
        }

        @Override
        AbstractPromise<?> tryFire(int mode) {
            AbstractPromise<U> out = output;

            if (!isDone0(out.resultHolder)) {
                AbstractPromise<V> in = input;
                Object inResult = in.resultHolder;

                if (inResult instanceof AltResult) {
                    out.completeRelayThrowable(inResult);
                } else {
                    try {
                        if (isSyncOrNestedMode(mode) && !executeDirectly()) {
                            return null;
                        }

                        if (out.setUncancellable()) {
                            V value = in.decodeValue(inResult);
                            out.completeValue(fn.apply(value));
                        }
                    } catch (Throwable ex) {
                        out.completeThrowable(ex);
                    }
                }
            }

            input = null;
            output = null;
            fn = null;

            return postFire(out, mode);
        }
    }

    static class UniCaching<V, X> extends AbstractUniCompletion<V, V> {

        Class<X> exceptionType;
        Function<? super X, ? extends V> fallback;

        UniCaching(Executor executor, AbstractPromise<V> input, AbstractPromise<V> output,
                   Class<X> exceptionType, Function<? super X, ? extends V> fallback) {
            super(executor, input, output);
            this.exceptionType = exceptionType;
            this.fallback = fallback;
        }

        @Override
        AbstractPromise<?> tryFire(int mode) {
            AbstractPromise<V> out = output;

            if (!isDone0(out.resultHolder)) {
                Object inResult = input.resultHolder;
                Throwable cause;

                if (inResult instanceof AltResult
                        && exceptionType.isInstance((cause = ((AltResult) inResult).cause))) {
                    try {
                        if (isSyncOrNestedMode(mode) && !executeDirectly()) {
                            return null;
                        }

                        if (out.setUncancellable()) {
                            @SuppressWarnings("unchecked") final X castException = (X) cause;
                            out.completeValue(fallback.apply(castException));
                        }
                    } catch (Throwable ex) {
                        out.completeThrowable(ex);
                    }
                } else {
                    out.completeRelay(inResult);
                }
            }

            input = null;
            output = null;
            exceptionType = null;
            fallback = null;

            return postFire(out, mode);
        }
    }

    static class UniHandle<V, U> extends AbstractUniCompletion<V, U> {

        BiFunction<? super V, ? super Throwable, ? extends U> fn;

        UniHandle(Executor executor, AbstractPromise<V> input, AbstractPromise<U> output,
                  BiFunction<? super V, ? super Throwable, ? extends U> fn) {
            super(executor, input, output);
            this.fn = fn;
        }

        @Override
        AbstractPromise<?> tryFire(int mode) {
            AbstractPromise<U> out = output;

            if (!isDone0(out.resultHolder)) {
                try {
                    if (isSyncOrNestedMode(mode) && !executeDirectly()) {
                        return null;
                    }

                    if (out.setUncancellable()) {
                        AbstractPromise<V> in = input;
                        Object inResult = in.resultHolder;
                        Throwable cause;
                        V value;

                        if (inResult instanceof AltResult) {
                            value = null;
                            cause = ((AltResult) inResult).cause;
                        } else {
                            value = in.decodeValue(inResult);
                            cause = null;
                        }

                        out.completeValue(fn.apply(value, cause));
                    }

                } catch (Throwable ex) {
                    out.completeThrowable(ex);
                }
            }

            input = null;
            output = null;
            fn = null;

            return postFire(out, mode);
        }

    }

    static class UniWhenComplete<V> extends AbstractUniCompletion<V, V> {

        BiConsumer<? super V, ? super Throwable> action;

        UniWhenComplete(Executor executor, AbstractPromise<V> input, AbstractPromise<V> output,
                        BiConsumer<? super V, ? super Throwable> action) {
            super(executor, input, output);
            this.action = action;
        }

        @Override
        AbstractPromise<?> tryFire(int mode) {
            AbstractPromise<V> out = output;

            if (!isDone0(out.resultHolder)) {
                try {
                    if (isSyncOrNestedMode(mode) && !executeDirectly()) {
                        return null;
                    }

                    if (out.setUncancellable()) {
                        AbstractPromise<V> in = input;
                        Object inResult = in.resultHolder;
                        Throwable cause;
                        V value;

                        if (inResult instanceof AltResult) {
                            value = null;
                            cause = ((AltResult) inResult).cause;
                        } else {
                            value = in.decodeValue(inResult);
                            cause = null;
                        }

                        action.accept(value, cause);

                        out.completeRelay(inResult);
                    }
                } catch (Throwable ex) {
                    // 这里的实现与JDK不同，这里仅仅是记录一个异常，不会传递给下一个Future
                    logger.warn("UniWhenComplete.action.accept caught exception", ex);

                    out.completeRelay(input.resultHolder);
                }
            }

            input = null;
            output = null;
            action = null;

            return postFire(out, mode);
        }

    }

    static class UniWhenExceptionally<V> extends AbstractUniCompletion<V, V> {

        Consumer<? super Throwable> action;

        UniWhenExceptionally(Executor executor, AbstractPromise<V> input, AbstractPromise<V> output,
                             Consumer<? super Throwable> action) {
            super(executor, input, output);
            this.action = action;
        }

        @Override
        AbstractPromise<?> tryFire(int mode) {
            AbstractPromise<V> out = output;

            if (!isDone0(out.resultHolder)) {
                Object inResult = input.resultHolder;

                if (inResult instanceof AltResult) {
                    try {
                        if (isSyncOrNestedMode(mode) && !executeDirectly()) {
                            return null;
                        }

                        if (out.setUncancellable()) {
                            action.accept(((AltResult) inResult).cause);
                            out.completeRelay(inResult);
                        }

                    } catch (Throwable ex) {
                        // 这里仅仅是记录一个异常，不会传递给下一个Future
                        logger.warn("UniWhenExceptionally.action.accept caught exception", ex);
                        out.completeRelay(inResult);
                    }
                } else {
                    out.completeRelay(inResult);
                }
            }

            input = null;
            output = null;
            action = null;

            return postFire(out, mode);
        }

    }

    static abstract class ListenCompletion<V> extends Completion {

        Executor executor;
        AbstractPromise<V> input;

        ListenCompletion(Executor executor, AbstractPromise<V> input) {
            this.executor = executor;
            this.input = input;
        }

        /**
         * 如果可以立即执行，则返回true，否则将提交到{@link #executor}稍后执行。
         */
        final boolean executeDirectly() {
            Executor e = executor;
            if (e == null || EventLoopUtils.inEventLoop(e)) {
                executor = null;
                return true;
            }

            e.execute(this);
            // help gc
            executor = null;
            return false;
        }

    }

    static class ListenWhenComplete<V> extends ListenCompletion<V> {

        BiConsumer<? super V, ? super Throwable> action;

        ListenWhenComplete(Executor executor, AbstractPromise<V> input,
                           BiConsumer<? super V, ? super Throwable> action) {
            super(executor, input);
            this.action = action;
        }

        @Override
        AbstractPromise<?> tryFire(int mode) {
            try {
                if (isSyncOrNestedMode(mode) && !executeDirectly()) {
                    return null;
                }

                AbstractPromise<V> in = input;
                Object inResult = in.resultHolder;
                Throwable cause;
                V value;

                if (inResult instanceof AltResult) {
                    value = null;
                    cause = ((AltResult) inResult).cause;
                } else {
                    value = in.decodeValue(inResult);
                    cause = null;
                }

                action.accept(value, cause);
            } catch (Throwable ex) {
                logger.warn("ListenWhenComplete.action.accept caught exception", ex);
            }

            input = null;
            action = null;

            return null;
        }
    }

    static class ListenWhenExceptionally<V> extends ListenCompletion<V> {

        Consumer<? super Throwable> action;

        ListenWhenExceptionally(Executor executor, AbstractPromise<V> input,
                                Consumer<? super Throwable> action) {
            super(executor, input);
            this.action = action;
        }

        @Override
        AbstractPromise<?> tryFire(int mode) {
            AbstractPromise<V> in = input;
            Object inResult = in.resultHolder;

            if (inResult instanceof AltResult) {
                try {
                    if (isSyncOrNestedMode(mode) && !executeDirectly()) {
                        return null;
                    }

                    Throwable cause = ((AltResult) inResult).cause;
                    action.accept(cause);
                } catch (Throwable ex) {
                    logger.warn("ListenWhenComplete.action.accept caught exception", ex);
                }
            }

            input = null;
            action = null;

            return null;
        }
    }


}
