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

import com.wjybxx.fastjgame.utils.CheckUtils;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * http监听端口配置信息
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/10/21
 * github - https://github.com/hl845740757
 */
public class HttpPortConfig {

    private final int httpSessionTimeout;
    private final HttpRequestDispatcher dispatcher;

    private HttpPortConfig(Builder builder) {
        this.httpSessionTimeout = builder.httpSessionTimeout;
        this.dispatcher = builder.dispatcher;
    }

    /**
     * @return httpSession超时时间 - 秒
     */
    public int getHttpSessionTimeout() {
        return httpSessionTimeout;
    }

    public HttpRequestDispatcher getDispatcher() {
        return dispatcher;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        private int httpSessionTimeout = 15;
        private HttpRequestDispatcher dispatcher;

        public Builder setHttpSessionTimeout(int httpSessionTimeout) {
            this.httpSessionTimeout = CheckUtils.requirePositive(httpSessionTimeout, "httpSessionTimeout");
            return this;
        }

        public Builder setDispatcher(@Nonnull HttpRequestDispatcher dispatcher) {
            this.dispatcher = dispatcher;
            return this;
        }

        public HttpPortConfig build() {
            checkParam();
            return new HttpPortConfig(this);
        }

        private void checkParam() {
            Objects.requireNonNull(dispatcher, "dispatcher");
        }
    }

}
