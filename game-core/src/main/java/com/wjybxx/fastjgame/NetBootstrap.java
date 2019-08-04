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
import com.wjybxx.fastjgame.concurrent.misc.AbstractThreadLifeCycleHelper;
import com.wjybxx.fastjgame.configwrapper.ArrayConfigWrapper;
import com.wjybxx.fastjgame.configwrapper.ConfigWrapper;
import com.wjybxx.fastjgame.configwrapper.MapConfigWrapper;

import java.util.HashSet;
import java.util.Set;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/4
 * github - https://github.com/hl845740757
 */
public class NetBootstrap<T extends NetBootstrap<T>> extends AbstractThreadLifeCycleHelper {

    private ConfigWrapper startArgs = MapConfigWrapper.EMPTY_MAP_WRAPPER;
    private int framesPerSecond = 20;
    private Set<AbstractModule> moduleList = new HashSet<>();

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
        moduleList.add(module);
        return self();
    }

    @Override
    protected void startImp() throws Exception {

    }

    @Override
    protected void shutdownImp() {

    }
}
