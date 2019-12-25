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

package com.wjybxx.fastjgame.mgr;

import com.google.inject.Inject;
import com.wjybxx.fastjgame.annotation.EventLoopSingleton;
import com.wjybxx.fastjgame.concurrent.EventLoop;
import com.wjybxx.fastjgame.misc.CuratorFacade;
import org.apache.curator.utils.CloseableExecutorService;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * curator管理器
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/12 12:05
 * github - https://github.com/hl845740757
 */
@EventLoopSingleton
@NotThreadSafe
public class CuratorMgr extends CuratorFacade {

    private final CuratorClientMgr curatorClientMgr;
    private final GameEventLoopMgr gameEventLoopMgr;

    @Inject
    public CuratorMgr(CuratorClientMgr curatorClientMgr, GameEventLoopMgr gameEventLoopMgr) {
        super(curatorClientMgr.getClient());
        this.curatorClientMgr = curatorClientMgr;
        this.gameEventLoopMgr = gameEventLoopMgr;
    }

    @Nonnull
    @Override
    protected CloseableExecutorService newClosableExecutorService() {
        return curatorClientMgr.newClosableExecutorService();
    }

    @Nonnull
    @Override
    protected EventLoop listenerExecutor() {
        return gameEventLoopMgr.getEventLoop();
    }
}
