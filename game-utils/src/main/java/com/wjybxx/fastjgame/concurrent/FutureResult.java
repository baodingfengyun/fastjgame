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

package com.wjybxx.fastjgame.concurrent;

/**
 * {@link ListenableFuture}对应的执行结果。
 * 在执行完毕后，可以获取该结果，从而代替传递future对象。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/1/8
 * github - https://github.com/hl845740757
 */
public interface FutureResult<V> {

    /**
     * 如果执行成功，则返回对应的结果，否则返回null。
     * 注意：
     * 如果future关联的task没有返回值(操作完成返回null)，此时不能根据返回值做任何判断。对于这种情况，
     * 你可以使用{@link #isSuccess()},作为更好的选择。
     */
    V get();

    /**
     * 如果执行失败，则返回执行失败的原因，否则返回null。
     */
    Throwable cause();

    /**
     * 查询执行是否成功
     */
    boolean isSuccess();

    /**
     * 查询是否被取消
     */
    boolean isCancelled();
}
