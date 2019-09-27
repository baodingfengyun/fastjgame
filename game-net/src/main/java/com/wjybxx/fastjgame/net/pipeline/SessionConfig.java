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

package com.wjybxx.fastjgame.net.pipeline;

import com.wjybxx.fastjgame.net.ProtocolCodec;
import com.wjybxx.fastjgame.net.ProtocolDispatcher;
import com.wjybxx.fastjgame.net.SessionLifecycleAware;

/**
 * sesion的一些配置
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/9/26
 * github - https://github.com/hl845740757
 */
public interface SessionConfig {

    /**
     * @return 生命周期回调
     */
    SessionLifecycleAware lifecycleAware();

    /**
     * @return 协议内容编解码器
     */
    ProtocolCodec codec();

    /**
     * @return 协议内容分发器
     */
    ProtocolDispatcher dispatcher();

    /**
     * @return 连接超时时间，毫秒
     */
    int getConnectTimeoutMs();

    /**
     * @return 异步rpc调用超时时间，毫秒
     */
    int getRpcCallbackTimeoutMs();

    /**
     * @return 同步rpc调用超时时间，毫秒
     */
    int getSyncRpcTimeoutMs();
}
