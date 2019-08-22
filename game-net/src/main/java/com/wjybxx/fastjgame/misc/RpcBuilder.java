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

import com.wjybxx.fastjgame.net.*;

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
     * 设置要进行rpc调用所在的session，你最好是获取session，session存在的情况下再通过代理工具类生成该builder。
     * @param session 关联的session
     * @return this
     */
    RpcBuilder<V> setSession(@Nonnull Session session);

    /**
     * 设置成功时执行的回调
     * @param callback 回调逻辑
     * @return this
     */
    RpcBuilder<V> ifSuccess(@Nonnull SucceedRpcCallback<V> callback);

    /**
     * 设置失败时执行的回调
     * @param callback 回调逻辑
     * @return this
     */
    RpcBuilder<V> ifFailure(@Nonnull FailedRpcCallback callback);

    /**
     * 设置无论成功还是失败都会执行的回调
     * @param callback 回调逻辑
     * @return this
     */
    RpcBuilder<V> any(@Nonnull RpcCallback callback);

    /**
     * 发送一个单向消息(通知)。
     */
    void send();

    /**
     * 执行异步rpc调用。
     * 注意：请确保设置了Session。
     */
    void invoke();

    /**
     * 执行异步调用并返回一个future。
     * 注意：请确保设置了Session。
     * @return future
     */
    RpcFuture execute();

    /**
     * 执行同步rpc调用，并直接获得结果。
     * 注意：请确保设置了Session。
     * @return result
     */
    RpcResponse sync();
}
