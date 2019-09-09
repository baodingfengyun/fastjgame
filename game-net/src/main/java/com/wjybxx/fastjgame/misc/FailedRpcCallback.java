/*
 *    Copyright 2019 wjybxx
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.wjybxx.fastjgame.misc;

import com.wjybxx.fastjgame.net.RpcCallback;
import com.wjybxx.fastjgame.net.RpcResponse;

/**
 * 只有失败才执行的回调。
 * 声明为接口而不是抽象类，是为了方便使用lambda表达式。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/19
 * github - https://github.com/hl845740757
 */
@FunctionalInterface
public interface FailedRpcCallback extends RpcCallback {

    @Override
    default void onComplete(RpcResponse rpcResponse) {
        if (!rpcResponse.isSuccess()) {
            onFailure(rpcResponse);
        }
    }

    /**
     * 当调用失败时
     *
     * @param rpcResponse rpc调用结果
     */
    void onFailure(RpcResponse rpcResponse);
}
