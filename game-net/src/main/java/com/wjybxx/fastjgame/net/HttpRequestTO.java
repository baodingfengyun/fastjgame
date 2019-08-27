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

package com.wjybxx.fastjgame.net;

import javax.annotation.concurrent.Immutable;

/**
 * http请求传输对象
 * @author wjybxx
 * @version 1.0
 * date - 2019/4/28 18:55
 * github - https://github.com/hl845740757
 */
@Immutable
@TransferObject
public final class HttpRequestTO {

    /**
     * 请求的资源路径
     */
    private final String path;
    /**
     * 请求参数
     */
    private final HttpRequestParam params;

    public HttpRequestTO(String path, HttpRequestParam params) {
        this.path = path;
        this.params = params;
    }

    public String getPath() {
        return path;
    }

    public HttpRequestParam getParams() {
        return params;
    }
}
