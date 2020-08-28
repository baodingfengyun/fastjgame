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

import com.wjybxx.fastjgame.util.CheckUtils;

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

    private final int sndBuffer;
    private final int rcvBuffer;
    private final int httpSessionTimeout;
    private final int readTimeout;
    private final HttpRequestDispatcher dispatcher;

    private HttpPortConfig(Builder builder) {
        this.sndBuffer = builder.sndBuffer;
        this.rcvBuffer = builder.rcvBuffer;
        this.httpSessionTimeout = builder.httpSessionTimeout;
        this.readTimeout = builder.readTimeout;
        this.dispatcher = builder.dispatcher;
    }

    public int getSndBuffer() {
        return sndBuffer;
    }

    public int getRcvBuffer() {
        return rcvBuffer;
    }

    /**
     * @return httpSession超时时间 - 秒
     */
    public int getHttpSessionTimeout() {
        return httpSessionTimeout;
    }

    /**
     * @return channel读超时 - 秒
     */
    public int getReadTimeout() {
        return readTimeout;
    }

    public HttpRequestDispatcher getDispatcher() {
        return dispatcher;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        private int sndBuffer = 8192;
        private int rcvBuffer = 8192;
        private int httpSessionTimeout = 10;
        private int readTimeout = 30;
        private HttpRequestDispatcher dispatcher;

        public Builder setSndBuffer(int sndBuffer) {
            this.sndBuffer = sndBuffer;
            return this;
        }

        public Builder setRcvBuffer(int rcvBuffer) {
            this.rcvBuffer = rcvBuffer;
            return this;
        }

        public Builder setHttpSessionTimeout(int httpSessionTimeout) {
            this.httpSessionTimeout = CheckUtils.requirePositive(httpSessionTimeout, "httpSessionTimeout");
            return this;
        }

        public Builder setReadTimeout(int readTimeout) {
            this.readTimeout = CheckUtils.requirePositive(readTimeout, "readTimeout");
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
