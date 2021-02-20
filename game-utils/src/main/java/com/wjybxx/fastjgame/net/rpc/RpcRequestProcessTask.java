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
package com.wjybxx.fastjgame.net.rpc;

import com.wjybxx.fastjgame.util.concurrent.FutureUtils;
import com.wjybxx.fastjgame.util.concurrent.ListenableFuture;
import com.wjybxx.fastjgame.util.concurrent.Promise;
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * rpc请求提交任务
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/8
 * github - https://github.com/hl845740757
 */
public class RpcRequestProcessTask implements ProcessTask {

    /**
     * 执行上下文
     */
    private final RpcProcessContext context;
    /**
     * 请求内容
     */
    private final Object request;
    /**
     * 返回结果的通道
     */
    private final Promise<?> promise;

    public RpcRequestProcessTask(RpcProcessContext context, Object request, Promise<?> promise) {
        this.context = context;
        this.promise = promise;
        this.request = request;
    }

    @Override
    public void run() {
        try {
            final Object result = context.session().config().processor().process(context, (RpcMethodSpec) request);
            if (result == null) {
                promise.trySuccess(null);
                return;
            }
            if (result instanceof ListenableFuture) {
                final ListenableFuture<?> future = (ListenableFuture<?>) result;
                setFuture(promise, future);
                return;
            }

            @SuppressWarnings("unchecked") final Promise<Object> castPromise = (Promise<Object>) promise;
            castPromise.trySuccess(result);
        } catch (Throwable e) {
            promise.tryFailure(e);
            // 虽然rpc调用可以捕获异常，但在处理之后应该重新抛出异常信息，避免异常丢失
            ExceptionUtils.rethrow(e);
        }
    }

    private static <V> void setFuture(Promise<?> promise, ListenableFuture<V> future) {
        @SuppressWarnings("unchecked") final Promise<V> castPromise = (Promise<V>) promise;
        FutureUtils.setFuture(castPromise, future);
    }
}
