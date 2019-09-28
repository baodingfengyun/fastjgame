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
import com.wjybxx.fastjgame.eventloop.NetEventLoop;
import com.wjybxx.fastjgame.module.NetEventLoopModule;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * NetEventLoop管理器，使得{@link NetEventLoopModule}中的管理器可以获取运行环境。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/3
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public class NetEventLoopManager {

    private NetEventLoop eventLoop;

    @Inject
    public NetEventLoopManager() {

    }

    /**
     * 不允许外部调用，保证安全性
     */
    public void publish(NetEventLoop eventLoop) {
        if (eventLoop.inEventLoop()) {
            this.eventLoop = eventLoop;
        } else {
            throw new IllegalStateException("internal api");
        }
    }

    public NetEventLoop eventLoop() {
        return eventLoop;
    }

    public boolean inEventLoop() {
        if (null == eventLoop) {
            throw new IllegalStateException();
        }
        return eventLoop.inEventLoop();
    }
}
