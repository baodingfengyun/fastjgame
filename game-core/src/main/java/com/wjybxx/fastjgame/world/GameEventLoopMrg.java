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

package com.wjybxx.fastjgame.world;

import com.google.inject.Inject;
import com.wjybxx.fastjgame.annotation.EventLoopSingleton;
import com.wjybxx.fastjgame.eventloop.NetEventLoop;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * 管理World所属的{@link GameEventLoop}。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/4
 * github - https://github.com/hl845740757
 */
@EventLoopSingleton
@NotThreadSafe
public class GameEventLoopMrg {

    private GameEventLoop eventLoop;

    @Inject
    public GameEventLoopMrg() {

    }

    public GameEventLoop getEventLoop() {
        return eventLoop;
    }

    public NetEventLoop getNetEventLoop() {
        return getEventLoop().netEventLoop();
    }

    void publish(@Nonnull GameEventLoop eventLoop) {
        if (this.eventLoop != null) {
            // 重复赋值？
            throw new IllegalStateException();
        }
        this.eventLoop = eventLoop;
    }
}
