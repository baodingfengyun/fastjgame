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

package com.wjybxx.fastjgame.manager;

import com.google.inject.Inject;
import com.wjybxx.fastjgame.concurrent.ListenableFuture;
import com.wjybxx.fastjgame.eventloop.NetEventLoop;

/**
 * 本地session管理器超类。
 * Q: 为何要有本地session？<br>
 * A: 使用{@link NetEventLoop}作为中间层，可以提供和远程一样的通信API，两个线程之间也可以使用rpc逻辑，而不是复杂的{@link ListenableFuture}。
 * <p>
 * 本地session没有ack超时逻辑，也不需要重发逻辑。
 * 仅仅只有建立连接、断开连接、发送消息。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/9/9
 * github - https://github.com/hl845740757
 */
public abstract class JVMSessionManager extends AbstractSessionManager {

    @Inject
    public JVMSessionManager(NetTimeManager netTimeManager) {
        super(netTimeManager);
    }

}
