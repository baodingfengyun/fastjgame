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

package com.wjybxx.fastjgame.net.injvm;

import com.wjybxx.fastjgame.misc.SessionLifecycleAware;
import com.wjybxx.fastjgame.net.DefaultSessionConfig;
import com.wjybxx.fastjgame.net.ProtocolCodec;
import com.wjybxx.fastjgame.net.ProtocolDispatcher;
import com.wjybxx.fastjgame.net.SessionConfig;

import javax.annotation.Nonnull;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2019/9/28
 * github - https://github.com/hl845740757
 */
public class DefaultJVMSessionConfig implements JVMSessionConfig {

    private final SessionConfig parentConfig;

    public DefaultJVMSessionConfig(SessionConfig parentConfig) {
        this.parentConfig = parentConfig;
    }

    @Override
    public SessionLifecycleAware lifecycleAware() {
        return parentConfig.lifecycleAware();
    }

    @Override
    public ProtocolCodec codec() {
        return parentConfig.codec();
    }

    @Override
    public ProtocolDispatcher dispatcher() {
        return parentConfig.dispatcher();
    }

    @Override
    public int getSessionTimeoutMs() {
        return parentConfig.getSessionTimeoutMs();
    }

    @Override
    public int getRpcCallbackTimeoutMs() {
        return parentConfig.getRpcCallbackTimeoutMs();
    }

    @Override
    public int getSyncRpcTimeoutMs() {
        return parentConfig.getSyncRpcTimeoutMs();
    }

    public static JVMSessionConfigBuilder newBuilder() {
        return new JVMSessionConfigBuilder();
    }

    public static class JVMSessionConfigBuilder {

        private final DefaultSessionConfig.SessionConfigBuilder parentBuilder = DefaultSessionConfig.newBuilder();

        public JVMSessionConfigBuilder setLifecycleAware(@Nonnull SessionLifecycleAware lifecycleAware) {
            parentBuilder.setLifecycleAware(lifecycleAware);
            return this;
        }

        public JVMSessionConfigBuilder setCodec(@Nonnull ProtocolCodec codec) {
            parentBuilder.setProtocolCodec(codec);
            return this;
        }

        public JVMSessionConfigBuilder setDispatcher(@Nonnull ProtocolDispatcher dispatcher) {
            parentBuilder.setProtocolDispatcher(dispatcher);
            return this;
        }

        public JVMSessionConfigBuilder setSessionTimeoutMs(int sessionTimeoutMs) {
            parentBuilder.setSessionTimeoutMs(sessionTimeoutMs);
            return this;
        }

        public JVMSessionConfigBuilder setRpcCallbackTimeoutMs(int rpcCallbackTimeoutMs) {
            parentBuilder.setRpcCallbackTimeoutMs(rpcCallbackTimeoutMs);
            return this;
        }

        public JVMSessionConfigBuilder setSyncRpcTimeoutMs(int syncRpcTimeoutMs) {
            parentBuilder.setSyncRpcTimeoutMs(syncRpcTimeoutMs);
            return this;
        }

        public JVMSessionConfig build() {
            return new DefaultJVMSessionConfig(parentBuilder.build());
        }
    }
}
