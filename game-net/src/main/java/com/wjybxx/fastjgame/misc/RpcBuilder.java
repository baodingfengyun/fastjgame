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

package com.wjybxx.fastjgame.misc;

import com.wjybxx.fastjgame.net.RpcCallback;
import com.wjybxx.fastjgame.net.RpcResponse;
import com.wjybxx.fastjgame.net.Session;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * 封装Rpc请求的一些细节，方便实现统一管控。其实把rpc调用看做多线程调用，就很容易理顺这些东西了。
 * <p>
 * Q: 为何提供该对象？
 * A: Net包提供的Rpc过于底层，很多接口并不为某一个具体的应用设计，虽然可以推荐某些使用方式，
 * 但仍然保留用户自定义的方式。{@link RpcBuilder}提供了一套更良好的api.
 * <p>
 * 注意：它并不是线程安全的，而只是提供更加容易使用的接口而已。
 * <p>
 * Q: 为何需要手动指定session？不能像常见的rpc那样直接获得一个proxy就行吗？
 * A: 对于一般应用而言，当出现多个服务提供者的时候，可以使用任意一个服务提供者，这样可以实现负载均衡。但是对于游戏而言，不行！
 * 对于游戏而言，每一个请求，每一个消息都是要发给确定的服务提供者的（另一个确定的服务器），因此你要获得一个正确的proxy并不容易，
 * 你必定需要指定一些额外参数才能获得正确的proxy。由于要获得正确的proxy，必定要获取正确的session，因此干脆不创建proxy，而是指定session。
 * <p>
 * Q: 为什么指定Session的时候可以为null？
 * A: 可以省却外部的检查。
 * <p>
 * 使用示例：
 * <pre>
 * 1. rpc调用：
 * {@code
 *      Proxy.methodName(a, b, c)
 *          .ifSuccess(result -> onSuccess(result))
 *          .call(session);
 * }
 * </pre>
 *
 * <pre>
 * 2.广播:
 * {@code
 *     Proxy.methodName(a, b, c)
 *          .broadcast(sessionCollection);
 * }
 * 等价于
 * {@code
 *      RpcBuilder builder = Proxy.methodName(a, b, c);
 *      for(Session session:sessionCollection) {
 *          builder.send(session);
 *      }
 * }
 * </pre>
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/22
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public interface RpcBuilder<V> {

    // ------------------------------------------- 添加回调 ----------------------------------------------

    /**
     * 设置成功时执行的回调。
     * 注意：如果你最后调用的是{@link #send(Session)}方法，那么该回调会被忽略。
     *
     * @param callback 回调逻辑
     * @return this
     */
    RpcBuilder<V> ifSuccess(@Nonnull SucceedRpcCallback<V> callback);

    /**
     * 设置成功时执行的回调。
     * 注意：如果你最后调用的是{@link #send(Session)}方法，那么该回调会被忽略。
     *
     * @param callback 回调逻辑 - 方法引用 {@code this::method}
     * @param context  存储的上下文
     * @return this
     */
    <T> RpcBuilder<V> ifSuccess(@Nonnull SaferSucceedRpcCallback<V, T> callback, @Nonnull T context);

    /**
     * 设置失败时执行的回调。
     * 注意：如果你最后调用的是{@link #send(Session)}方法，那么该回调会被忽略。
     *
     * @param callback 回调逻辑
     * @return this
     */
    RpcBuilder<V> ifFailure(@Nonnull FailedRpcCallback callback);

    /**
     * 设置失败时执行的回调。
     * 注意：如果你最后调用的是{@link #send(Session)}方法，那么该回调会被忽略。
     *
     * @param callback 回调逻辑 - 方法引用 {@code this::method}
     * @param context  存储的上下文
     * @return this
     */
    <T> RpcBuilder<V> ifFailure(@Nonnull SaferFailedRpcCallback<T> callback, @Nonnull T context);

    /**
     * 设置无论成功还是失败都会执行的回调。
     * 注意：如果你最后调用的是{@link #send(Session)}方法，那么该回调会被忽略。
     *
     * @param callback 回调逻辑
     * @return this
     */
    RpcBuilder<V> any(@Nonnull RpcCallback callback);

    /**
     * 设置无论成功还是失败都会执行的回调。
     * 注意：如果你最后调用的是{@link #send(Session)}方法，那么该回调会被忽略。
     *
     * @param callback 回调逻辑 - 方法引用 {@code this::method}
     * @param context  存储的上下文
     * @return this
     */
    <T> RpcBuilder<V> any(@Nonnull SaferRpcCallback<T> callback, @Nonnull T context);

    // --------------------------------------------- 真正执行 --------------------------------------------------

    /**
     * 发送一个单向通知 - 它也可以调用rpc方法，不过结果不会被传输回来。
     *
     * @param session 要通知的session
     * @return this 可以继续发送
     * @throws IllegalStateException 如果调用过send、broadcast以外的请求方法，则会抛出异常
     */
    RpcBuilder<V> send(@Nullable Session session) throws IllegalStateException;

    /**
     * 广播一个通知，它是对{@link #send(Session)}的一个包装。
     * 注意:
     * 1. 一旦调用了send方法，那么便不可以调用<b>send、broadcast</b>以外的请求方法。
     * 2. 即使添加了回调，这些回调也会被忽略。
     *
     * @param sessionIterable 要广播的所有session
     * @return this 可以继续发送
     * @throws IllegalStateException 如果调用过send、broadcast以外的请求方法，则会抛出异常。
     */
    RpcBuilder<V> broadcast(@Nullable Iterable<Session> sessionIterable) throws IllegalStateException;

    /**
     * 执行异步rpc调用，无论如何对方都会返回一个结果。
     * call是不是很好记住？那就多用它。
     * <p>
     * 注意：一旦调用了call方法，那么该builder便不可以再使用。
     *
     * @param session rpc请求的目的地，可以为null，以省却调用时的外部检查。
     * @throws IllegalStateException 如果重用一个可监听的rpcBuilder，则会抛出异常！
     */
    void call(@Nullable Session session) throws IllegalStateException;

    /**
     * 执行同步rpc调用，并直接获得结果。如果添加了回调，回调会在返回前执行。
     * <p>
     * 注意：
     * 1. 一旦调用了sync方法，那么该builder便不可以再使用。
     * 2. 少使用同步调用，必要的时候使用同步可以降低编程复杂度，但是大量使用会大大降低吞吐量。
     * 3. 一旦调用了syncCall方法，那么该builder便不可以再使用。
     * 4. 即使添加了回调，这些回调也会被忽略。
     *
     * @param session rpc请求的目的地，可以为null，以省却调用时的外部检查。
     * @return result
     * @throws IllegalStateException 如果重用一个可监听的rpcBuilder，则会抛出异常！
     */
    @Nonnull
    RpcResponse sync(@Nullable Session session) throws IllegalStateException;

    /**
     * 执行同步rpc调用，如果执行成功，则返回对应的调用结果，否则返回null。
     * <p>
     * 注意：
     * 1. 如果null是一个合理的返回值，那么你不能基于调用结果做出任何判断。这种情况下，建议你使用{@link #sync(Session)}，可以获得调用的结果码。
     * 2. 少使用同步调用，必要的时候使用同步可以降低编程复杂度，但是大量使用会大大降低吞吐量。
     * 3. 一旦调用了syncCall方法，那么该builder便不可以再使用。
     * 4. 即使添加了回调，这些回调也会被忽略。
     *
     * @param session rpc请求的目的地，可以为null，以省却调用时的外部检查。
     * @return result
     * @throws IllegalStateException 如果重用一个可监听的rpcBuilder，则会抛出异常！
     */
    @Nullable
    V syncCall(@Nullable Session session) throws IllegalStateException;

}
