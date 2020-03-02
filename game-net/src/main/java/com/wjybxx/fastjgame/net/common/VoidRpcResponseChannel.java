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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

/**
 * 没有返回值的Channel，占位符，表示用户不关心返回值或方法本身无返回值。
 * 主要用于实现单向通知。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/21
 * github - https://github.com/hl845740757
 */
@ThreadSafe
public class VoidRpcResponseChannel implements RpcResponseChannel<Object> {

    private static final RpcResponseChannel INSTANCE = new VoidRpcResponseChannel();

    @SuppressWarnings("unchecked")
    public static <T> RpcResponseChannel<T> getInstance() {
        return INSTANCE;
    }

    @Override
    public void writeSuccess(@Nullable Object body) {
        // do nothing
    }

    @Override
    public void writeFailure(@Nonnull RpcErrorCode errorCode, @Nonnull Throwable cause) {
        // do nothing
    }

    @Override
    public void writeFailure(@Nonnull RpcErrorCode errorCode, @Nonnull String message) {
        // do nothing
    }

    @Override
    public void write(@Nonnull RpcResponse rpcResponse) {
        // do nothing
    }

    @Override
    public boolean isVoid() {
        return true;
    }
}
