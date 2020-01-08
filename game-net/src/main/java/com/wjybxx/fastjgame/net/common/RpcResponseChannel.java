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

import com.wjybxx.fastjgame.utils.DebugUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

/**
 * 返回rpc结果的通道。
 * 注意：该channel是一次性的，只可以使用一次(返回一次结果)，多次调用将抛出异常。
 * 当该参数在Rpc方法的参数中出现时，代码生成工具会捕获泛型T的类型，作为返回类型，且{@link RpcResponseChannel}不会出现在代理方法参数中。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/3
 * github - https://github.com/hl845740757
 */
@ThreadSafe
public interface RpcResponseChannel<T> {

    /**
     * 返回rpc调用结果，表示调用成功，{@link #write(RpcResponse)}的快捷方式。
     *
     * @param body rpc调用结果/可能为null
     */
    default void writeSuccess(@Nullable T body) {
        if (null == body) {
            write(RpcResponse.SUCCESS);
        } else {
            write(RpcResponse.newSucceedResponse(body));
        }
    }

    /**
     * 返回rpc调用结果，表示调用失败，{@link #write(RpcResponse)}的快捷方式。
     *
     * @param errorCode rpc调用错误码
     * @param cause     造成失败的异常
     */
    default void writeFailure(@Nonnull RpcErrorCode errorCode, @Nonnull Throwable cause) {
        final String message = getCauseMessage(cause);
        writeFailure(errorCode, message);
    }

    /**
     * 返回rpc调用结果，表示调用失败，{@link #write(RpcResponse)}的快捷方式。
     *
     * @param errorCode rpc调用错误码
     * @param message   错误信息
     */
    default void writeFailure(@Nonnull RpcErrorCode errorCode, @Nonnull String message) {
        write(RpcResponse.newFailResponse(errorCode, message));
    }

    /**
     * 返回rpc调用结果，表示调用失败，{@link #write(RpcResponse)}的快捷方式。
     *
     * @param cause 造成失败的异常
     */
    default void writeFailure(@Nonnull Throwable cause) {
        write(RpcResponse.newFailResponse(RpcErrorCode.SERVER_EXCEPTION, getCauseMessage(cause)));
    }

    /**
     * 返回rpc调用结果，表示调用失败，{@link #write(RpcResponse)}的快捷方式。
     *
     * @param message 错误信息
     */
    default void writeFailure(@Nonnull String message) {
        write(RpcResponse.newFailResponse(RpcErrorCode.SERVER_EXCEPTION, message));
    }

    /**
     * 返回rpc调用结果。
     *
     * @param rpcResponse rpc调用结果
     */
    void write(@Nonnull RpcResponse rpcResponse);

    /**
     * 是否不关心结果，true表示不关心结果
     *
     * @return true/false
     */
    boolean isVoid();

    static String getCauseMessage(@Nonnull Throwable cause) {
        final String message;
        if (DebugUtils.isDebugOpen()) {
            // debug开启情况下，返回详细信息
            message = ExceptionUtils.getStackTrace(cause);
        } else {
            message = ExceptionUtils.getRootCauseMessage(cause);
        }
        return message;
    }
}
