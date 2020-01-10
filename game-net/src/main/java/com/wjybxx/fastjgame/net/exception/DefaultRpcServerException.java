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

package com.wjybxx.fastjgame.net.exception;

import com.wjybxx.fastjgame.net.common.RpcErrorCode;
import com.wjybxx.fastjgame.net.common.RpcResponse;

/**
 * 服务器执行调用时出现异常 - 通过{@link com.wjybxx.fastjgame.net.common.RpcResponse}解析得到。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/1/8
 * github - https://github.com/hl845740757
 */
public class DefaultRpcServerException extends RpcServerException {

    private final RpcErrorCode errorCode;

    public DefaultRpcServerException(RpcResponse rpcResponse) {
        super((String) rpcResponse.getBody());
        this.errorCode = rpcResponse.getErrorCode();
    }

    @Override
    public RpcErrorCode getErrorCode() {
        return errorCode;
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        // 本地堆栈没有意义
        return this;
    }
}
