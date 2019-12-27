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
import com.wjybxx.fastjgame.annotation.EventLoopSingleton;
import com.wjybxx.fastjgame.mgr.*;
import com.wjybxx.fastjgame.world.World;

/**
 * WorldModule，游戏world的顶层module。
 * 线程级单例。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/12 12:06
 * github - https://github.com/hl845740757
 */
@EventLoopSingleton
public abstract class WorldModule extends AbstractModule {

    // 这样改造之后为典型的模板方法
    @Override
    protected final void configure() {
        binder().requireExplicitBindings();
        configCore();

        bindWorldAndWorldInfoMgr();

        bindOthers();
    }

    private void configCore() {
        bind(ProtocolCodecMgr.class).in(Singleton.class);
        bind(GameAcceptorMgr.class).in(Singleton.class);

        bind(NetContextMgr.class).in(Singleton.class);
        bind(HttpDispatcherMgr.class).in(Singleton.class);

        bind(WorldTimeMgr.class).in(Singleton.class);
        bind(WorldTimerMgr.class).in(Singleton.class);

        bind(WorldWrapper.class).in(Singleton.class);

        bind(GameEventLoopMgr.class).in(Singleton.class);
        bind(CuratorMgr.class).in(Singleton.class);
        bind(GuidMgr.class).to(ZkGuidMgr.class).in(Singleton.class);
        bind(WorldEventMgr.class).in(Singleton.class);

        // 表格读取 （如果表格全是不可变对象，那么可能是多线程模块中的）
        bind(TemplateMgr.class).in(Singleton.class);

        // redis支持
        bind(RedisMgr.class).in(Singleton.class);
    }

    /**
     * 请注意绑定{@link World}类和{@link WorldInfoMgr}
     */
    protected abstract void bindWorldAndWorldInfoMgr();

    /**
     * 绑定其它的类
     */
    protected abstract void bindOthers();
}
