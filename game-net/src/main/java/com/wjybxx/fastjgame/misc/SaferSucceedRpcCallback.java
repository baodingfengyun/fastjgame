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

import com.wjybxx.fastjgame.net.common.RpcResponse;

/**
 * 更安全的成功时才回调的rpc回调
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/9/21
 * github - https://github.com/hl845740757
 */
@FunctionalInterface
public interface SaferSucceedRpcCallback<V, T> extends SaferRpcCallback<T> {

    @SuppressWarnings("unchecked")
    @Override
    default void onComplete(RpcResponse rpcResponse, T context) {
        if (rpcResponse.isSuccess()) {
            onSuccess((V) rpcResponse.getBody(), context);
        }
    }

    /**
     * 当执行成功时
     *
     * @param result 调用结果
     */
    void onSuccess(V result, T context);

}
