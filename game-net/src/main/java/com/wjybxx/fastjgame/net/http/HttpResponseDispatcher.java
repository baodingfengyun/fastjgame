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

package com.wjybxx.fastjgame.net.http;

import okhttp3.Call;
import okhttp3.Response;

import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * http回调分发器 - 使得用户能够监测回调逻辑，不让回调悄悄的执行，悄悄的执行会导致一些问题。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/10/1
 * github - https://github.com/hl845740757
 */
public interface HttpResponseDispatcher {

    /**
     * 当请求成功时
     *
     * @param callback 回调逻辑
     * @param call     请求信息
     * @param response 用户不必调用{@link Response#close()}方法，底层会自动调用。
     * @throws Exception error
     */
    void post(OkHttpCallback callback, @Nonnull Call call, @Nonnull Response response) throws Exception;

    /**
     * 当请求失败时
     *
     * @param callback 回调逻辑
     * @param call     请求信息
     * @param cause    失败的原因
     * @throws Exception error
     */
    void post(OkHttpCallback callback, @Nonnull Call call, @Nonnull IOException cause) throws Exception;
}
