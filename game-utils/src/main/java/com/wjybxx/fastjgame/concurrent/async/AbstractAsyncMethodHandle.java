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

package com.wjybxx.fastjgame.concurrent.async;

/**
 * 异步方法句柄的抽象实现。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/1/5 20:22
 * github - https://github.com/hl845740757
 */
public abstract class AbstractAsyncMethodHandle<T, V> implements AsyncMethodHandle<T, V> {

    @Override
    public final AsyncMethodListenable<V> call(T typeObj) {
        final DefaultAsyncMethodListenable<V> listener = new DefaultAsyncMethodListenable<>();
        callImp(typeObj, listener);
        return listener;
    }

    /**
     * 子类真正实现异步调用，并绑定回调。
     *
     * @param typeObj  方法的执行对象
     * @param callback 回调逻辑，子类需要绑定该回调。
     */
    protected abstract void callImp(T typeObj, AsyncMethodCallback<? super V> callback);

}
