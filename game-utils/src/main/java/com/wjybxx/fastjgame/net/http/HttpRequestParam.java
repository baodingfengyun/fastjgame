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

import com.wjybxx.fastjgame.util.config.Params;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * http请求参数
 * (不要随便挪动位置：注解处理器用到了完整类名)
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/27
 * github - https://github.com/hl845740757
 */
public class HttpRequestParam extends Params {

    /**
     * 协议版本
     */
    private final HttpVersion version;
    /**
     * 请求头信息
     */
    private final HttpHeaders headers;
    /**
     * 请求类型
     */
    private final HttpMethod method;
    /**
     * 请求参数
     * name -> value
     * (post也是如此)
     */
    private final Map<String, String> params;

    public HttpRequestParam(HttpVersion version, HttpHeaders headers, HttpMethod method, Map<String, String> params) {
        this.version = version;
        this.headers = headers;
        this.method = method;
        this.params = Collections.unmodifiableMap(params);
    }

    @Override
    public Set<String> keys() {
        return params.keySet();
    }

    @Override
    public String getAsString(String key) {
        return params.get(key);
    }

    public HttpVersion getVersion() {
        return version;
    }

    public HttpHeaders getHeaders() {
        return headers;
    }

    public HttpMethod getMethod() {
        return method;
    }

    public Map<String, String> getParams() {
        return params;
    }

    @Override
    public String toString() {
        return "HttpRequestParam{" +
                "version=" + version +
                ", headers=" + headers +
                ", method=" + method +
                ", params=" + params +
                "} " + super.toString();
    }
}
