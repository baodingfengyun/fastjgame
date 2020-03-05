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

package com.wjybxx.fastjgame.utils.concurrent;

import javax.annotation.Nonnull;
import java.util.concurrent.Executor;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/3/4
 */

public class VoidPromise extends VoidFuture implements Promise<Object> {

    // ------------------------------------- 赋值操作不造成任何影响 -----------------------------

    @Override
    public void setSuccess(Object result) {

    }

    @Override
    public boolean trySuccess(Object result) {
        return false;
    }

    @Override
    public void setFailure(@Nonnull Throwable cause) {

    }

    @Override
    public boolean tryFailure(@Nonnull Throwable cause) {
        return false;
    }

    @Override
    public boolean setUncancellable() {
        return true;
    }

    // -------------------------------------- 流式语法的支持 --------------------------------------

    @Override
    public Promise<Object> await() throws InterruptedException {
        super.await();
        return this;
    }

    @Override
    public Promise<Object> awaitUninterruptibly() {
        super.awaitUninterruptibly();
        return this;
    }

    @Override
    public Promise<Object> addListener(@Nonnull FutureListener<? super Object> listener) {
        super.addListener(listener);
        return this;
    }

    @Override
    public Promise<Object> addListener(@Nonnull FutureListener<? super Object> listener, @Nonnull Executor bindExecutor) {
        super.addListener(listener, bindExecutor);
        return this;
    }

    @Override
    public Promise<Object> removeListener(@Nonnull FutureListener<? super Object> listener) {
        super.removeListener(listener);
        return this;
    }

}
