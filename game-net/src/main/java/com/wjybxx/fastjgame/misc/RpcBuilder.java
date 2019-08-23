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
 * 使用示例：
 * <pre>
 * {@code
 *      Proxy.methodName(a, b, c)
 *          .setSession(session)
 *          .ifSuccess(result -> onSuccess(result))
 *          .execute();
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
     * 查看该builder绑定的{@link RpcCall}的信息，不可以修改。
     * @return call
     */
    @Nonnull
    RpcCall<V> getCall();

    /**
     * 设置要进行rpc调用所在的session，必须调用一次进行赋值(即使设置为null)，否则发送时抛出异常。
     *
     * @apiNote
     * 由于用户可能并不清楚对应的session存在与否，因此当session为null时，必须安全的失败。
     * @param session 关联的session
     * @return this
     */
    RpcBuilder<V> setSession(@Nullable Session session);

    /**
     * 设置成功时执行的回调。
     *
     * @param callback 回调逻辑
     * @return this
     * @throws UnsupportedOperationException 如果返回值类型为void或Void则抛出该异常。
     */
    RpcBuilder<V> ifSuccess(@Nonnull SucceedRpcCallback<V> callback) throws UnsupportedOperationException;

    /**
     * 设置失败时执行的回调
     * @param callback 回调逻辑
     * @return this
     * @throws UnsupportedOperationException 如果返回值类型为void或Void则抛出该异常。
     */
    RpcBuilder<V> ifFailure(@Nonnull FailedRpcCallback callback) throws UnsupportedOperationException;

    /**
     * 设置无论成功还是失败都会执行的回调
     * @param callback 回调逻辑
     * @return this
     * @throws UnsupportedOperationException 如果未调用{@link #setSession(Session)}或重复发送请求则抛出异常。
     */
    RpcBuilder<V> any(@Nonnull RpcCallback callback) throws UnsupportedOperationException;

    /**
     * 执行调用，如果添加了回调，则发起rpc调用，如果没有回调(不关心返回值)，则发起通知。
     * @throws IllegalStateException 如果未调用{@link #setSession(Session)}或重复发送请求则抛出异常。
     */
    void execute() throws IllegalStateException;

    /**
     * 执行异步Rpc调用并返回一个future。
     * @return future
     * @throws IllegalStateException 一个builder不可以反复调用发送接口。
     * @throws UnsupportedOperationException 如果返回值类型为void或Void则抛出该异常。
     */
    RpcFuture submit() throws IllegalStateException, UnsupportedOperationException;

    /**
     * 执行同步rpc调用，并直接获得结果。如果添加了回调，回调会在返回前执行。
     * @return result
     * @throws IllegalStateException 如果未调用{@link #setSession(Session)}或重复发送请求则抛出异常。
     * @throws UnsupportedOperationException 如果返回值类型为void或Void则抛出该异常。
     */
    RpcResponse sync() throws IllegalStateException, UnsupportedOperationException;
}
