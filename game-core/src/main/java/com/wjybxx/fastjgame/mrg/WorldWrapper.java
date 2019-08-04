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

/**
 * world需要的控制器的包装类，避免子类的构造方法出现大量对象
 * @author wjybxx
 * @version 1.0
 * date - 2019/4/27 22:12
 * github - https://github.com/hl845740757
 */
public class WorldWrapper {

    private final CodecHelperMrg codecHelperMrg;
    private final CuratorClientMrg curatorClientMrg;
    private final CuratorMrg curatorMrg;
    private final GameConfigMrg gameConfigMrg;
    private final GameEventLoopMrg gameEventLoopMrg;
    private final GlobalExecutorMrg globalExecutorMrg;
    private final HttpDispatcherMrg httpDispatcherMrg;
    private final MessageDispatcherMrg messageDispatcherMrg;
    private final InnerAcceptorMrg innerAcceptorMrg;
    private final NetContextManager netContextManager;
    private final SystemTimeMrg systemTimeMrg;
    private final TemplateMrg templateMrg;
    private final TimerMrg timerMrg;
    private final WorldInfoMrg worldInfoMrg;
    private final GuidMrg guidMrg;

    @Inject
    public WorldWrapper(GameEventLoopMrg gameEventLoopMrg, WorldInfoMrg worldInfoMrg, SystemTimeMrg systemTimeMrg,
                        MessageDispatcherMrg messageDispatcherMrg, HttpDispatcherMrg httpDispatcherMrg,
                        CodecHelperMrg codecHelperMrg, TimerMrg timerMrg, GlobalExecutorMrg globalExecutorMrg,
                        CuratorMrg curatorMrg, GuidMrg guidMrg, GameConfigMrg gameConfigMrg, NetContextManager netContextManager, CuratorClientMrg curatorClientMrg, TemplateMrg templateMrg, InnerAcceptorMrg innerAcceptorMrg) {
        this.gameEventLoopMrg = gameEventLoopMrg;
        this.worldInfoMrg = worldInfoMrg;
        this.systemTimeMrg = systemTimeMrg;
        this.messageDispatcherMrg = messageDispatcherMrg;
        this.httpDispatcherMrg = httpDispatcherMrg;
        this.codecHelperMrg = codecHelperMrg;
        this.timerMrg = timerMrg;
        this.globalExecutorMrg = globalExecutorMrg;
        this.curatorMrg = curatorMrg;
        this.guidMrg = guidMrg;
        this.gameConfigMrg = gameConfigMrg;
        this.netContextManager = netContextManager;
        this.curatorClientMrg = curatorClientMrg;
        this.templateMrg = templateMrg;
        this.innerAcceptorMrg = innerAcceptorMrg;
    }

    public GameEventLoopMrg getGameEventLoopMrg() {
        return gameEventLoopMrg;
    }

    public WorldInfoMrg getWorldInfoMrg() {
        return worldInfoMrg;
    }

    public SystemTimeMrg getSystemTimeMrg() {
        return systemTimeMrg;
    }

    public MessageDispatcherMrg getMessageDispatcherMrg() {
        return messageDispatcherMrg;
    }

    public HttpDispatcherMrg getHttpDispatcherMrg() {
        return httpDispatcherMrg;
    }

    public CodecHelperMrg getCodecHelperMrg() {
        return codecHelperMrg;
    }

    public TimerMrg getTimerMrg() {
        return timerMrg;
    }

    public GlobalExecutorMrg getGlobalExecutorMrg() {
        return globalExecutorMrg;
    }

    public CuratorMrg getCuratorMrg() {
        return curatorMrg;
    }

    public GuidMrg getGuidMrg() {
        return guidMrg;
    }

    public GameConfigMrg getGameConfigMrg() {
        return gameConfigMrg;
    }

    public NetContextManager getNetContextManager() {
        return netContextManager;
    }

    public CuratorClientMrg getCuratorClientMrg() {
        return curatorClientMrg;
    }

    public TemplateMrg getTemplateMrg() {
        return templateMrg;
    }

    public InnerAcceptorMrg getInnerAcceptorMrg() {
        return innerAcceptorMrg;
    }
}
