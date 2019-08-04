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

package com.wjybxx.fastjgame;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.wjybxx.fastjgame.concurrent.misc.AbstractThreadLifeCycleHelper;
import com.wjybxx.fastjgame.configwrapper.ArrayConfigWrapper;
import com.wjybxx.fastjgame.configwrapper.ConfigWrapper;
import com.wjybxx.fastjgame.configwrapper.MapConfigWrapper;
import com.wjybxx.fastjgame.mrg.WorldInfoMrg;
import com.wjybxx.fastjgame.utils.MathUtils;
import com.wjybxx.fastjgame.world.GameEventLoopGroup;
import com.wjybxx.fastjgame.world.World;

import java.util.HashSet;
import java.util.Set;

/**
 * 启动引导
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/4
 * github - https://github.com/hl845740757
 */
public class Bootstrap<T extends Bootstrap<T>> extends AbstractThreadLifeCycleHelper {

    private final AbstractModule globalModule;
    private final GameEventLoopGroup gameEventLoopGroup;

    private ConfigWrapper startArgs = MapConfigWrapper.EMPTY_MAP_WRAPPER;
    private int framesPerSecond = 20;
    private Set<AbstractModule> moduleSet = new HashSet<>();

    public Bootstrap(AbstractModule globalModule, GameEventLoopGroup gameEventLoopGroup) {
        this.globalModule = globalModule;
        this.gameEventLoopGroup = gameEventLoopGroup;
        moduleSet.add(globalModule);
    }

    @SuppressWarnings("unchecked")
    protected T self() {
        return (T) this;
    }

    public T setArgs(String[] args) {
        startArgs = new ArrayConfigWrapper(args);
        return self();
    }

    public T setArgs(ConfigWrapper startArgs) {
        this.startArgs = startArgs;
        return self();
    }

    public T setFramesPerSecond(int framesPerSecond) {
        this.framesPerSecond = framesPerSecond;
        return self();
    }

    public ConfigWrapper getStartArgs() {
        return startArgs;
    }

    public int getFramesPerSecond() {
        return framesPerSecond;
    }

    public T addModule(AbstractModule module) {
        moduleSet.add(module);
        return self();
    }

    @Override
    protected void startImp() throws Exception {
        Injector injector = Guice.createInjector(moduleSet);
        WorldInfoMrg worldInfoMrg = injector.getInstance(WorldInfoMrg.class);
        worldInfoMrg.init(startArgs, framesPerSecond);

        World world = injector.getInstance(World.class);
        gameEventLoopGroup.registerWorld(world, MathUtils.frameInterval(framesPerSecond));
    }

    @Override
    protected void shutdownImp() {

    }
}
