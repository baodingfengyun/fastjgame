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

package com.wjybxx.fastjgame.net.misc;

import com.wjybxx.fastjgame.net.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

/**
 * 默认的Http请求分发器实现
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/27
 * github - https://github.com/hl845740757
 */
public class DefaultHttpRequestDispatcher implements HttpRequestHandlerRegistry, HttpRequestDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(DefaultHttpRequestDispatcher.class);

    private final Map<String, HttpRequestHandler> handlerMap = new HashMap<>(64);

    @Override
    public final void register(@Nonnull String path, @Nonnull HttpRequestHandler httpRequestHandler) {
        final HttpRequestHandler existHandler = handlerMap.get(path);
        if (null == existHandler) {
            handlerMap.put(path, httpRequestHandler);
        } else {
            if (existHandler instanceof CompositeHttpRequestHandler) {
                ((CompositeHttpRequestHandler) existHandler).addHandler(httpRequestHandler);
            } else {
                handlerMap.put(path, new CompositeHttpRequestHandler(existHandler, httpRequestHandler));
            }
        }
    }

    @Override
    public void release() {
        handlerMap.clear();
    }

    @Override
    public final void post(HttpSession httpSession, String path, HttpRequestParam params) {
        HttpRequestHandler httpRequestHandler = handlerMap.get(path);
        if (null == httpRequestHandler) {
            // 未注册的路径
            httpSession.writeAndFlush(HttpResponseHelper.newNotFoundResponse());
            logger.warn("unregistered path {}", path);
            return;
        }
        // 分发请求
        try {
            httpRequestHandler.onHttpRequest(httpSession, path, params);
        } catch (Exception e) {
            logger.warn("handle path {} caught exception.", path, e);
        }
    }
}
