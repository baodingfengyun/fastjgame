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

package com.wjybxx.fastjgame.net;

import com.wjybxx.fastjgame.concurrent.EventLoop;
import com.wjybxx.fastjgame.concurrent.FutureListener;
import com.wjybxx.fastjgame.concurrent.ListenableFuture;

import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;

/**
 * Rpc调用的future。
 * 注意：在该Future上<b>主动获取结果</b>会打乱对方发送的消息之间的处理时序，你必须清除它可能带来的影响，否则不要轻易的主动获取结果！
 * 方法：{@link #get()}{@link #get(long, TimeUnit)} {@link #getNow()}
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/3
 * github - https://github.com/hl845740757
 * @apiNote Rpc请求具有时效性，因此{@link #get()},{@link #await()}系列方法，不会无限阻塞，都会在超时时间到达后醒来。
 */
public interface RpcFuture extends ListenableFuture<RpcResponse> {

    /**
     * {@inheritDoc}
     * 默认执行在发起rpc调用的用户所在线程
     *
     * @param listener 要添加的监听器。PECS Listener作为消费者，可以把生产的结果V 看做V或V的超类型消费，因此需要使用super。
     */
    @Override
    void addListener(@Nonnull FutureListener<? super RpcResponse> listener);

    /**
     * 添加rpc调用回调，默认执行在发起rpc调用的用户所在的线程。
     *
     * @param rpcCallback rpc回调逻辑
     */
    void addCallback(RpcCallback rpcCallback);

    /**
     * 添加rpc调用回调，并指定运行环境。
     *
     * @param rpcCallback rpc回调逻辑
     * @param eventLoop   rpc回调的执行环境
     */
    void addCallback(RpcCallback rpcCallback, EventLoop eventLoop);

    // 1. RPCFuture上不会有执行失败异常，通过错误码来表示
    // 2. 在RpcFuture上不会无限阻塞，一定会在超时时间到了之后就醒来
    @Override
    RpcResponse get() throws InterruptedException;

    @Override
    RpcResponse get(long timeout, @Nonnull TimeUnit unit) throws InterruptedException;

    @Override
    void await() throws InterruptedException;

    @Override
    void awaitUninterruptibly();

    @Override
    boolean await(long timeout, TimeUnit unit) throws InterruptedException;

    @Override
    boolean awaitUninterruptibly(long timeout, TimeUnit unit);
}
