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

package com.wjybxx.fastjgame.module;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.wjybxx.fastjgame.annotation.EventLoopGroupSingleton;
import com.wjybxx.fastjgame.mgr.*;
import com.wjybxx.fastjgame.world.GameEventLoopGroup;

/**
 * {@link GameEventLoopGroup}级别的单例。
 * 这里的控制器完成一些多线程的逻辑，主要还是为了减少资源消耗 - 内存、网络、线程等。
 * 这里的资源在{@link GameEventLoopGroup}退出的时候完成释放。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/4
 * github - https://github.com/hl845740757
 */
@EventLoopGroupSingleton
public class WorldGroupModule extends AbstractModule {

    @Override
    protected void configure() {
        binder().requireExplicitBindings();

        bind(GameConfigMgr.class).in(Singleton.class);
        bind(GlobalExecutorMgr.class).in(Singleton.class);
        bind(CuratorClientMgr.class).in(Singleton.class);
        bind(LocalPortMgr.class).in(Singleton.class);
        bind(LogProducerMgr.class).in(Singleton.class);
        bind(RedisEventLoopMgr.class).in(Singleton.class);
    }
}
