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

package com.wjybxx.fastjgame.net.common;

import com.wjybxx.fastjgame.concurrent.EventLoop;
import com.wjybxx.fastjgame.concurrent.FutureListener;
import com.wjybxx.fastjgame.concurrent.SucceededFuture;

import javax.annotation.Nonnull;

/**
 * 已完成的Rpc调用，在它上面的任何监听都将立即执行。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/3
 * github - https://github.com/hl845740757
 */
public class CompletedRpcFuture extends SucceededFuture<RpcResponse> implements RpcFuture {

    /**
     * @param executor    用户所在EventLoop,为什么可以只使用用户线程？因为不会阻塞。
     * @param rpcResponse rpc结果
     */
    public CompletedRpcFuture(@Nonnull EventLoop executor, @Nonnull RpcResponse rpcResponse) {
        super(executor, rpcResponse);
    }

    // ------------------------------------------------ 流式语法支持 ------------------------------------

    @Override
    public RpcFuture await() {
        return (CompletedRpcFuture) super.await();
    }

    @Override
    public RpcFuture awaitUninterruptibly() {
        return (RpcFuture) super.awaitUninterruptibly();
    }

    @Override
    public RpcFuture addListener(@Nonnull FutureListener<? super RpcResponse> listener) {
        return (RpcFuture) super.addListener(listener);
    }

    @Override
    public RpcFuture addListener(@Nonnull FutureListener<? super RpcResponse> listener, @Nonnull EventLoop bindExecutor) {
        return (RpcFuture) super.addListener(listener, bindExecutor);
    }

    @Override
    public RpcFuture removeListener(@Nonnull FutureListener<? super RpcResponse> listener) {
        return (RpcFuture) super.removeListener(listener);
    }

}
