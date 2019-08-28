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
import com.wjybxx.fastjgame.net.RpcFuture;
import com.wjybxx.fastjgame.net.RpcResponse;
import com.wjybxx.fastjgame.net.Session;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * 封装Rpc请求的一些细节，方便实现统一管控。
 * Q: 为何提供该对象？
 * A:1. Net包提供的Rpc过于底层，很多接口并不为某一个具体的应用设计，虽然可以推荐某些使用方式，
 *      当仍然保留用户自定义的方式。
 *   2. 用户可以通过Builder进行在pipeline模式与session模式之间方便的切换，
 *      而不破坏既有代码。
 *
 * 注意：它并不是线程安全的，而只是提供更加容易使用的接口而已。
 *
 * Q: 为何需要手动指定session？不能像常见的rpc那样直接获得一个proxy就行吗？
 * A: 对于一般应用而言，当出现多个服务提供者的时候，可以使用任意一个服务提供者，这样可以实现负载均衡。但是对于游戏而言，不行！
 * 对于游戏而言，每一个请求，每一个消息都是要发给确定的服务提供者的（另一个确定的服务器），因此你要获得一个正确的proxy并不容易，
 * 你必定需要指定一些额外参数才能获得正确的proxy。由于要获得正确的proxy，必定要获取正确的session，因此干脆不创建proxy，而是指定session。
 *
 * Q: 为什么指定Session的时候可以为null？
 * A: 可以省却外部的检查。
 *
 * 使用示例：
 * <pre>
 * {@code
 *      Proxy.methodName(a, b, c)
 *          .ifSuccess(result -> onSuccess(result))
 *          .execute(session);
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

    /**
     * 是否可以监听，当rpc方法没有返回值时不可以监听。
     * 当方法参数中没有{@link com.wjybxx.fastjgame.net.RpcResponseChannel}，且方法的返回值类型为void时，该调用不可以监听。
     * 或者查看生成的代理中是true还是false。
     * 注意：如果可监听，那么该{@link RpcBuilder}不可以重用，如果不可以监听，那么该{@link RpcBuilder}可以重用。
     *
     * @return 当且仅当该调用没有返回值的时候返回false。
     */
    boolean isListenable();

    // ------------------------------------------- 添加回调 ----------------------------------------------
    /**
     * 设置成功时执行的回调。
     *
     * @param callback 回调逻辑
     * @return this
     * @throws UnsupportedOperationException 如果没有返回值(不可以监听)将抛出该异常。
     */
    RpcBuilder<V> ifSuccess(@Nonnull SucceedRpcCallback<V> callback) throws UnsupportedOperationException;

    /**
     * 设置失败时执行的回调
     * @param callback 回调逻辑
     * @return this
     * @throws UnsupportedOperationException 如果没有返回值(不可以监听)将抛出该异常。
     */
    RpcBuilder<V> ifFailure(@Nonnull FailedRpcCallback callback) throws UnsupportedOperationException;

    /**
     * 设置无论成功还是失败都会执行的回调
     * @param callback 回调逻辑
     * @return this
     * @throws UnsupportedOperationException 如果没有返回值(不可以监听)将抛出该异常。
     */
    RpcBuilder<V> any(@Nonnull RpcCallback callback) throws UnsupportedOperationException;

    // --------------------------------------------- 真正执行 --------------------------------------------------

    /**
     * 发送一个单向通知，不关心返回结果。
     * 注意：即使发送前添加了回调，这些回调也会被忽略。
     *
     * @param session rpc请求的目的地，可以为null，以省却调用时的外部检查。
     * @throws IllegalStateException 如果重用一个可监听的rpcBuilder，则会抛出异常！
     */
    void send(@Nullable Session session) throws IllegalStateException;

    /**
     * 执行异步rpc调用。
     * (名字实在不好起)
     *
     * @param session rpc请求的目的地，可以为null，以省却调用时的外部检查。
     * @throws IllegalStateException 如果重用一个可监听的rpcBuilder，则会抛出异常！
     */
    void execute(@Nullable Session session) throws IllegalStateException;

    /**
     * 执行异步Rpc调用并返回一个future。
     * (名字实在不好起)
     *
     * @return future
     * @throws IllegalStateException 如果重用一个可监听的rpcBuilder，则会抛出异常！
     * @throws UnsupportedOperationException 如果没有返回值(不可以监听)将抛出该异常。
     * @param session rpc请求的目的地，可以为null。
     */
    RpcFuture submit(@Nullable Session session) throws IllegalStateException, UnsupportedOperationException;

    /**
     * 执行同步rpc调用，如果执行成功，则返回对应的调用结果，否则返回null。
     * 注意：
     * 1. 如果null是一个合理的返回值，那么你不能基于调用结果做出任何判断。这种情况下，建议你使用{@link #sync(Session)}，可以获得调用的结果码。
     * 2. 如果添加了回调，回调会在返回前执行。
     *
     * @param session rpc请求的目的地，可以为null，以省却调用时的外部检查。
     * @return result
     * @throws IllegalStateException 如果重用一个可监听的rpcBuilder，则会抛出异常！
     * @throws UnsupportedOperationException 如果没有返回值(不可以监听)将抛出该异常。
     */
    @Nullable
    V call(@Nullable Session session) throws IllegalStateException, UnsupportedOperationException;

    /**
     * 执行同步rpc调用，并直接获得结果。如果添加了回调，回调会在返回前执行。
     * @param session rpc请求的目的地，可以为null，以省却调用时的外部检查。
     * @return result
     * @throws IllegalStateException 如果重用一个可监听的rpcBuilder，则会抛出异常！
     * @throws UnsupportedOperationException 如果没有返回值(不可以监听)将抛出该异常。
     */
    RpcResponse sync(@Nullable Session session) throws IllegalStateException, UnsupportedOperationException;
}
