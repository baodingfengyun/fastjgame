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

import io.netty.channel.Channel;

/**
 * Http事件信息
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/1
 * github - https://github.com/hl845740757
 */
public class HttpRequestEvent {

    /**
     * 事件对应的channel
     */
    private final Channel channel;
    /**
     * 请求的资源路径
     */
    private final String path;
    /**
     * 请求参数
     */
    private final HttpRequestParam params;
    /**
     * 端口上的一些信息
     */
    private final HttpPortContext portExtraInfo;

    public HttpRequestEvent(Channel channel, String path, HttpRequestParam params, HttpPortContext portExtraInfo) {
        this.channel = channel;
        this.path = path;
        this.params = params;
        this.portExtraInfo = portExtraInfo;
    }

    public Channel channel() {
        return channel;
    }

    public String getPath() {
        return path;
    }

    public HttpRequestParam getParams() {
        return params;
    }

    public HttpPortContext getPortExtraInfo() {
        return portExtraInfo;
    }
}
