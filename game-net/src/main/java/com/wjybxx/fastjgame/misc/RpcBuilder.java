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

import com.wjybxx.fastjgame.net.common.RpcCall;
import com.wjybxx.fastjgame.net.common.RpcCallback;
import com.wjybxx.fastjgame.net.session.Session;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.concurrent.ExecutionException;

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
 *          .onSuccess(result -> onSuccess(result))
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
     * 注意：只有当后续调用的是{@link #call(Session)}时才会有效。
     *
     * @param callback 回调逻辑
     * @return this
     */
    RpcBuilder<V> onSuccess(@Nonnull SucceededRpcCallback<V> callback);

    /**
     * 设置失败时执行的回调。
     * 注意：只有当后续调用的是{@link #call(Session)}时才会有效。
     *
     * @param callback 回调逻辑
     * @return this
     */
    RpcBuilder<V> onFailure(@Nonnull FailedRpcCallback<V> callback);

    /**
     * 设置无论成功还是失败都会执行的回调。
     * 注意：只有当后续调用的是{@link #call(Session)}时才会有效。
     *
     * @param callback 回调逻辑
     * @return this
     */
    RpcBuilder<V> onComplete(@Nonnull RpcCallback<V> callback);

    // --------------------------------------------- 真正执行 --------------------------------------------------

    /**
     * 发送一个单向消息 - 它表示不需要方法的执行结果。
     * 注意:
     * 1. 即使添加了回调，这些回调也会被忽略。
     *
     * @param session 要通知的session
     */
    void send(@Nonnull Session session);

    /**
     * 广播一个消息，它是对{@link #send(Session)}的一个包装。
     * 注意:
     * 1. 即使添加了回调，这些回调也会被忽略。
     *
     * @param sessionGroup 要广播的所有session
     */
    void broadcast(@Nonnull Iterable<Session> sessionGroup);

    /**
     * 执行异步rpc调用，无论如何对方都会返回一个结果。
     * call是不是很好记住？那就多用它。
     * <p>
     * 注意：
     * 1. 一旦调用了call方法，回调信息将被重置。
     * 2. 没有设置回调，则表示不关心结果。
     *
     * @param session rpc请求的目的地
     */
    void call(@Nonnull Session session);

    /**
     * 执行同步rpc调用，如果执行成功，则返回对应的调用结果，否则返回null。
     * <p>
     * 注意：
     * 1. 少使用同步调用，必要的时候使用同步可以降低编程复杂度，但是大量使用会大大降低吞吐量。
     * 2. 即使添加了回调，这些回调也会被忽略。
     *
     * @param session rpc请求的目的地
     * @return result
     */
    V syncCall(@Nonnull Session session) throws ExecutionException;

    // ------------------------------------------  路由/转发 ------------------------------------------

    /**
     * 获取该方法包含的调用信息，可用于二次封装。
     * 警告：不可修改对象的内容，否则可能引发bug。
     */
    RpcCall<V> getCall();

    /**
     * 设置路由策略。
     *
     * @param router 路由器
     * @return this
     */
    RpcBuilder<V> router(RpcRouter<V> router);

}
