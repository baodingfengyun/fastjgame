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

package com.wjybxx.fastjgame.net.exception;

import com.wjybxx.fastjgame.net.rpc.RpcErrorCode;

/**
 * 找不到对于的session异常
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/3/8
 * github - https://github.com/hl845740757
 */
public class RpcSessionNotFoundException extends RpcLocalException {

    public static final RpcSessionNotFoundException INSTANCE = new RpcSessionNotFoundException();

    private RpcSessionNotFoundException() {
        // 不填充堆栈
        super(null, null, false, false);
    }

    @Override
    public RpcErrorCode getErrorCode() {
        return RpcErrorCode.LOCAL_SESSION_NOT_FOUND;
    }

}
