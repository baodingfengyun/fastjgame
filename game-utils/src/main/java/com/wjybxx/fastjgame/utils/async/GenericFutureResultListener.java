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

package com.wjybxx.fastjgame.utils.async;

import com.wjybxx.fastjgame.utils.concurrent.FutureResult;

/**
 * {@link FutureResult}的监听器
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/1/8
 * github - https://github.com/hl845740757
 */
@FunctionalInterface
public interface GenericFutureResultListener<FR extends FutureResult<V>, V> {

    /**
     * 当future对应的操作完成时，该方法将被调用。
     *
     * @param futureResult 执行结果
     */
    void onComplete(FR futureResult);

}
