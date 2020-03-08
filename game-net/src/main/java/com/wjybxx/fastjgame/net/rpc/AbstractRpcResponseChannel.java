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

import com.wjybxx.fastjgame.utils.DebugUtils;
import com.wjybxx.fastjgame.utils.SystemUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * RpcResponseChannel的骨架实现
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/16
 * github - https://github.com/hl845740757
 */
public abstract class AbstractRpcResponseChannel<T> implements RpcResponseChannel<T> {

    protected AbstractRpcResponseChannel() {

    }

    @Override
    public boolean trySuccess(@Nullable T result) {
        return write(RpcErrorCode.SUCCESS, result);
    }

    @Override
    public void tryFailure(@Nonnull RpcErrorCode errorCode, @Nonnull String message) {
        if (errorCode == RpcErrorCode.SUCCESS) {
            throw new IllegalArgumentException("failure error code can't be SUCCESS");
        }
        write(errorCode, message);
    }

    private boolean write(RpcErrorCode errorCode, Object body) {
        if (writable == null || writable.compareAndSet(true, false)) {
            doWrite(errorCode, body);
            return true;
        } else {
            throw new IllegalStateException("ResponseChannel can't be reused!");
        }
    }

    /**
     * 子类真正的进行发送
     */
    protected abstract void doWrite(RpcErrorCode errorCode, Object body);

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
