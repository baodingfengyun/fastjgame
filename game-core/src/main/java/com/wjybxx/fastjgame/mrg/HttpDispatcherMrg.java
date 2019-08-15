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

package com.wjybxx.fastjgame.mrg;

import com.google.inject.Inject;
import com.wjybxx.fastjgame.annotation.WorldSingleton;
import com.wjybxx.fastjgame.configwrapper.ConfigWrapper;
import com.wjybxx.fastjgame.misc.HttpResponseHelper;
import com.wjybxx.fastjgame.net.HttpRequestHandler;
import com.wjybxx.fastjgame.net.HttpSession;
import com.wjybxx.fastjgame.utils.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.HashMap;
import java.util.Map;

/**
 * 实现http请求的分发操作。
 * 注意：不同的world有不同的消息处理器，单例级别为World级别。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/4
 * github - https://github.com/hl845740757
 */
@WorldSingleton
@NotThreadSafe
public class HttpDispatcherMrg implements HttpRequestHandler {

    private static final Logger logger = LoggerFactory.getLogger(HttpDispatcherMrg.class);

    private final Map<String, HttpRequestHandler> handlerMap = new HashMap<>();

    @Inject
    public HttpDispatcherMrg() {

    }

    public void registerHandler(String path, HttpRequestHandler httpRequestHandler) {
        CollectionUtils.requireNotContains(handlerMap, path, "path");
        handlerMap.put(path, httpRequestHandler);
    }

    @Override
    public void onHttpRequest(HttpSession httpSession, String path, ConfigWrapper requestParams) throws Exception {
        HttpRequestHandler httpRequestHandler = handlerMap.get(path);
        if (null == httpRequestHandler){
            // 未注册的路径
            httpSession.writeAndFlush(HttpResponseHelper.newNotFoundResponse());
            logger.warn("unregistered path {}", path);
            return;
        }

        // 分发请求
        try {
            httpRequestHandler.onHttpRequest(httpSession, path, requestParams);
        } catch (Exception e) {
            logger.warn("handle path {} caught exception.", path, e);
        }
    }
}
