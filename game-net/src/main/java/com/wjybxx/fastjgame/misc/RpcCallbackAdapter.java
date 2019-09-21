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

/**
 * 适配器模式的一个运用。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/9/21
 * github - https://github.com/hl845740757
 */
public class RpcCallbackAdapter<T> implements RpcCallback {

    private final SaferRpcCallback<T> saferRpcCallback;

    private final T context;

    public RpcCallbackAdapter(SaferRpcCallback<T> saferRpcCallback, T context) {
        this.saferRpcCallback = saferRpcCallback;
        this.context = context;
    }

    @Override
    public void onComplete(RpcResponse rpcResponse) {
        saferRpcCallback.onComplete(rpcResponse, context);
    }
}
