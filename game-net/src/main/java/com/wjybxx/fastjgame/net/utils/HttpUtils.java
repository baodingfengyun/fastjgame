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

package com.wjybxx.fastjgame.net.utils;

import java.util.Map;

/**
 * Http辅助类
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/12/30
 * github - https://github.com/hl845740757
 */
public class HttpUtils {

    /**
     * 构建get请求的参数部分
     *
     * @param url    远程地址
     * @param params get请求参数
     * @return full request
     */
    public static String buildGetUrl(String url, Map<String, String> params) {
        String safeUrl = checkUrl(url);
        StringBuilder builder = new StringBuilder(safeUrl);
        // 是否添加&符号
        boolean appendAnd = false;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (appendAnd) {
                builder.append("&");
            }
            builder.append(entry.getKey()).append("=").append(entry.getValue());
            appendAnd = true;
        }
        return builder.toString();
    }

    /**
     * 检查url格式，默认http协议
     *
     * @param url 待检查的url
     * @return 正确的url格式
     */
    private static String checkUrl(final String url) {
        String safeUrl;
        if (url.startsWith("http://") || url.startsWith("https://")) {
            safeUrl = url;
        } else {
            safeUrl = "http://" + url;
        }
        // 末尾添加参数之前添加?
        if (safeUrl.charAt(safeUrl.length() - 1) != '?') {
            safeUrl = safeUrl + "?";
        }
        return safeUrl;
    }

}
