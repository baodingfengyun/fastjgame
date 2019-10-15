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

package com.wjybxx.fastjgame.net.socket;

import com.wjybxx.fastjgame.net.session.SessionConfig;
import com.wjybxx.fastjgame.utils.CheckUtils;

/**
 * socket连接配置
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/9/26
 * github - https://github.com/hl845740757
 */
public final class SocketSessionConfig extends SessionConfig {

    /**
     * 发送缓冲区
     */
    private final int sndBuffer;
    /**
     * 接收缓冲区
     */
    private final int rcvBuffer;
    /**
     * 最大帧长度
     */
    private final int maxFrameLength;
    /**
     * 建立连接超时时间 - 毫秒
     */
    private final int connectTimeoutMs;
    /**
     * socket读超时时间 - 秒
     */
    private final int readTimeout;
    /**
     * 是否开启断线重连/消息确认机制
     */
    private final boolean autoReconnect;

    private SocketSessionConfig(SocketSessionConfigBuilder builder) {
        super(builder);
        this.sndBuffer = builder.sndBuffer;
        this.rcvBuffer = builder.rcvBuffer;
        this.maxFrameLength = builder.maxFrameLength;
        this.connectTimeoutMs = builder.connectTimeoutMs;
        this.readTimeout = builder.readTimeout;
        this.autoReconnect = builder.autoReconnect;
    }

    /**
     * @return socket发送缓冲区大小
     */
    public int sndBuffer() {
        return sndBuffer;
    }

    /**
     * @return socket接收缓冲区大小
     */
    public int rcvBuffer() {
        return rcvBuffer;
    }

    /**
     * @return 允许的最大帧长度
     */
    public int maxFrameLength() {
        return maxFrameLength;
    }

    /**
     * @return 建立连接超时时间- 毫秒
     */
    public int connectTimeoutMs() {
        return connectTimeoutMs;
    }

    /**
     * @return 读超时时间 - 秒
     */
    public int readTimeout() {
        return readTimeout;
    }

    /**
     * @return 是否开启了断线重连/消息确认机制
     */
    public boolean isAutoReconnect() {
        return autoReconnect;
    }

    public static SocketSessionConfigBuilder newBuilder() {
        return new SocketSessionConfigBuilder();
    }

    public static class SocketSessionConfigBuilder extends SessionConfigBuilder<SocketSessionConfigBuilder> {

        private int sndBuffer = 64 * 1024;
        private int rcvBuffer = 64 * 1024;
        private int maxFrameLength = 8 * 1024;
        private int connectTimeoutMs = 30 * 1000;
        private int readTimeout = 45;
        private boolean autoReconnect = false;

        @Override
        protected void checkParams() {
            super.checkParams();
        }

        public SocketSessionConfigBuilder setSndBuffer(int sndBuffer) {
            CheckUtils.checkPositive(sndBuffer, "sndBuffer");
            this.sndBuffer = sndBuffer;
            return this;
        }

        public SocketSessionConfigBuilder setRcvBuffer(int rcvBuffer) {
            CheckUtils.checkPositive(rcvBuffer, "rcvBuffer");
            this.rcvBuffer = rcvBuffer;
            return this;
        }

        public SocketSessionConfigBuilder setMaxFrameLength(int maxFrameLength) {
            CheckUtils.checkPositive(maxFrameLength, "maxFrameLength");
            this.maxFrameLength = maxFrameLength;
            return this;
        }

        public SocketSessionConfigBuilder setConnectTimeoutMs(int connectTimeoutMs) {
            this.connectTimeoutMs = connectTimeoutMs;
            return this;
        }

        public SocketSessionConfigBuilder setReadTimeout(int readTimeout) {
            this.readTimeout = readTimeout;
            return this;
        }

        public SocketSessionConfigBuilder setAutoReconnect(boolean autoReconnect) {
            this.autoReconnect = autoReconnect;
            return this;
        }

        @Override
        public SocketSessionConfig build() {
            return new SocketSessionConfig(this);
        }
    }
}
