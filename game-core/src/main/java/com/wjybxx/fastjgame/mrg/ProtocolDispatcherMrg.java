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
import com.wjybxx.fastjgame.annotation.EventLoopSingleton;
import com.wjybxx.fastjgame.misc.DefaultProtocolDispatcher;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * 协议分发管理器。
 * 实现TCP/Ws长链接的 [单向消息] 和 [rpc请求] 的分发。
 * 注意：不同的world有不同的协议处理器，单例级别为world级别。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/4
 * github - https://github.com/hl845740757
 */
@EventLoopSingleton
@NotThreadSafe
public class ProtocolDispatcherMrg extends DefaultProtocolDispatcher {

    @Inject
    public ProtocolDispatcherMrg() {
    }
}
