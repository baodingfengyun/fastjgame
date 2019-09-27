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

package com.wjybxx.fastjgame.net;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * 某个端口的绑定信息 - 它解释如何处理该端口上接收到的连接和消息。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/9/10
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public class PortContext {

    public final SessionLifecycleAware lifecycleAware;
    public final ProtocolDispatcher protocolDispatcher;

    public PortContext(SessionLifecycleAware lifecycleAware, ProtocolDispatcher protocolDispatcher) {
        this.lifecycleAware = lifecycleAware;
        this.protocolDispatcher = protocolDispatcher;
    }
}
