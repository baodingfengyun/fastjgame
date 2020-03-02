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

import com.wjybxx.fastjgame.utils.SystemUtils;

import javax.annotation.Nonnull;
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

    /**
     * 默认检查{@link #writable}标记。<br>
     * 如果有大量的RPC调用，可以选择关闭检查，以减少volatile带来的内存同步开销。
     * 其实影响不大，测试期间一定要开着，可以帮助你排除一些错误。
     */
    private static final boolean CHECK_WRITABLE = SystemUtils.getProperties().getAsBool("AbstractRpcResponseChannel.CHECK_WRITABLE", true);

    private final AtomicBoolean writable;

    protected AbstractRpcResponseChannel() {
        writable = CHECK_WRITABLE ? new AtomicBoolean(true) : null;
    }

    @Override
    public final void write(@Nonnull RpcResponse rpcResponse) {
        if (writable == null || writable.compareAndSet(true, false)) {
            doWrite(rpcResponse);
        } else {
            throw new IllegalStateException("ResponseChannel can't be reused!");
        }
    }

    /**
     * 子类真正的进行发送
     *
     * @param rpcResponse rpc响应
     */
    protected abstract void doWrite(RpcResponse rpcResponse);

    @Override
    public final boolean isVoid() {
        return false;
    }
}
