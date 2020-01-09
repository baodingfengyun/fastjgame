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

package com.wjybxx.fastjgame.async;

import com.wjybxx.fastjgame.concurrent.GenericFutureResultListener;
import com.wjybxx.fastjgame.concurrent.timeout.TimeoutFutureResult;

import javax.annotation.Nonnull;
import java.util.concurrent.ExecutionException;

/**
 * 具有超时的异步(远程)服务句柄。
 * 子实现类的所有异步/同步方法调用必须在限定时间内完成(失败或成功)。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/1/9
 * github - https://github.com/hl845740757
 */
public interface TimeoutServiceHandle<T extends MethodSpec<?>, FR extends TimeoutFutureResult<?>> extends ServiceHandle<T, FR> {

    @Override
    void execute(@Nonnull T methodSpec);

    @Override
    <V> void call(@Nonnull T methodSpec, GenericFutureResultListener<FR> listener);

    @Override
    <V> V syncCall(@Nonnull T methodSpec) throws ExecutionException;

}
