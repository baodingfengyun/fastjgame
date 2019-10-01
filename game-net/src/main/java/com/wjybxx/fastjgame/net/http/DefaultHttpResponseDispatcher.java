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
 * 默认的Http回调分发器
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/10/1
 * github - https://github.com/hl845740757
 */
public class DefaultHttpResponseDispatcher implements HttpResponseDispatcher {

    @Override
    public void post(OkHttpCallback callback, @Nonnull Call call, @Nonnull Response response) throws IOException {
        callback.onResponse(call, response);
    }

    @Override
    public void post(OkHttpCallback callback, @Nonnull Call call, @Nonnull IOException cause) {
        callback.onFailure(call, cause);
    }
}
