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
 * 异步方法回调
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/1/5 18:53
 * github - https://github.com/hl845740757
 */
@FunctionalInterface
public interface AsyncMethodCallback<V> {

    /**
     * 当对应的异步方法执行完毕时，该方法将会被调用。
     *
     * @param asyncMethodResult 方法的执行结果。
     */
    void onComplete(AsyncMethodResult<? extends V> asyncMethodResult);

}
