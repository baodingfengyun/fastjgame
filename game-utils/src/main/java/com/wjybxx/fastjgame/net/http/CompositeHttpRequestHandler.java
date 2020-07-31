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

import java.util.ArrayList;
import java.util.List;

/**
 * 为多个http请求处理器提供一个单一的视图
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/27
 * github - https://github.com/hl845740757
 */
public class CompositeHttpRequestHandler implements HttpRequestHandler {

    private final List<HttpRequestHandler> children = new ArrayList<>(2);

    public CompositeHttpRequestHandler(HttpRequestHandler first, HttpRequestHandler second) {
        children.add(first);
        children.add(second);
    }

    public CompositeHttpRequestHandler addHandler(HttpRequestHandler handler) {
        children.add(handler);
        return this;
    }

    @Override
    public void onHttpRequest(HttpSession httpSession, String path, HttpRequestParam params) throws Exception {
        for (HttpRequestHandler httpRequestHandler : children) {
            DefaultHttpRequestDispatcher.invokeHandlerSafely(httpSession, path, params, httpRequestHandler);
        }
    }
}
