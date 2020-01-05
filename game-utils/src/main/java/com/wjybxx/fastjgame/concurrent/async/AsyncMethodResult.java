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
 * 异步方法执行结果。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/1/5 21:31
 * github - https://github.com/hl845740757
 */
public class AsyncMethodResult<V> {

    /**
     * 方法的执行结果。如果null是一个正常的返回值，那么需要根据 {@code cause}判断是否成功。
     */
    private final V result;
    /**
     * 造成方法失败的原因，如果该值不为null，表示执行失败。
     */
    private final Throwable cause;

    AsyncMethodResult(V result, Throwable cause) {
        this.result = result;
        this.cause = cause;
    }

    public V getResult() {
        return result;
    }

    public Throwable getCause() {
        return cause;
    }

    public boolean isSuccess() {
        return cause == null;
    }

    public boolean isFailure() {
        return cause != null;
    }

    @Override
    public String toString() {
        return "AsyncMethodResult{" +
                "result=" + result +
                ", cause=" + cause +
                '}';
    }
}
