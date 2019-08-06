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
import com.wjybxx.fastjgame.annotation.WorldSingleton;
import com.wjybxx.fastjgame.world.GameEventLoopMrg;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * world需要的控制器的包装类，避免子类的构造方法出现大量对象
 * @author wjybxx
 * @version 1.0
 * date - 2019/4/27 22:12
 * github - https://github.com/hl845740757
 */
@WorldSingleton
@NotThreadSafe
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
    private final NetContextMrg netContextMrg;
    private final WorldTimeMrg worldTimeMrg;
    private final TemplateMrg templateMrg;
    private final WorldTimerMrg worldTimerMrg;
    private final WorldInfoMrg worldInfoMrg;
    private final GuidMrg guidMrg;

    @Inject
    public WorldWrapper(GameEventLoopMrg gameEventLoopMrg, WorldInfoMrg worldInfoMrg, WorldTimeMrg worldTimeMrg,
                        MessageDispatcherMrg messageDispatcherMrg, HttpDispatcherMrg httpDispatcherMrg,
                        CodecHelperMrg codecHelperMrg, WorldTimerMrg worldTimerMrg, GlobalExecutorMrg globalExecutorMrg,
                        CuratorMrg curatorMrg, GuidMrg guidMrg, GameConfigMrg gameConfigMrg, NetContextMrg netContextMrg, CuratorClientMrg curatorClientMrg, TemplateMrg templateMrg, InnerAcceptorMrg innerAcceptorMrg) {
        this.gameEventLoopMrg = gameEventLoopMrg;
        this.worldInfoMrg = worldInfoMrg;
        this.worldTimeMrg = worldTimeMrg;
        this.messageDispatcherMrg = messageDispatcherMrg;
        this.httpDispatcherMrg = httpDispatcherMrg;
        this.codecHelperMrg = codecHelperMrg;
        this.worldTimerMrg = worldTimerMrg;
        this.globalExecutorMrg = globalExecutorMrg;
        this.curatorMrg = curatorMrg;
        this.guidMrg = guidMrg;
        this.gameConfigMrg = gameConfigMrg;
        this.netContextMrg = netContextMrg;
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

    public WorldTimeMrg getWorldTimeMrg() {
        return worldTimeMrg;
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

    public WorldTimerMrg getWorldTimerMrg() {
        return worldTimerMrg;
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

    public NetContextMrg getNetContextMrg() {
        return netContextMrg;
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
