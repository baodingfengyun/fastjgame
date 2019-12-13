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

import javax.annotation.concurrent.NotThreadSafe;

/**
 * world需要的控制器的包装类，避免子类的构造方法出现大量对象
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/4/27 22:12
 * github - https://github.com/hl845740757
 */
@EventLoopSingleton
@NotThreadSafe
public class WorldWrapper {

    private final ProtocolCodecMgr protocolCodecMgr;
    private final CuratorClientMgr curatorClientMgr;
    private final CuratorMgr curatorMgr;
    private final GameConfigMgr gameConfigMgr;
    private final GameEventLoopMgr gameEventLoopMgr;
    private final GlobalExecutorMgr globalExecutorMgr;
    private final HttpDispatcherMgr httpDispatcherMgr;
    private final ProtocolDispatcherMgr protocolDispatcherMgr;
    private final GameAcceptorMgr gameAcceptorMgr;
    private final NetContextMgr netContextMgr;
    private final WorldTimeMgr worldTimeMgr;
    private final TemplateMgr templateMgr;
    private final WorldTimerMgr worldTimerMgr;
    private final WorldInfoMgr worldInfoMgr;
    private final GuidMgr guidMgr;
    private final RedisMgr redisMgr;

    @Inject
    public WorldWrapper(GameEventLoopMgr gameEventLoopMgr, WorldInfoMgr worldInfoMgr, WorldTimeMgr worldTimeMgr,
                        ProtocolDispatcherMgr protocolDispatcherMgr, HttpDispatcherMgr httpDispatcherMgr,
                        ProtocolCodecMgr protocolCodecMgr, WorldTimerMgr worldTimerMgr, GlobalExecutorMgr globalExecutorMgr,
                        CuratorMgr curatorMgr, GuidMgr guidMgr, GameConfigMgr gameConfigMgr, NetContextMgr netContextMgr,
                        CuratorClientMgr curatorClientMgr, TemplateMgr templateMgr, GameAcceptorMgr gameAcceptorMgr,
                        RedisMgr redisMgr) {
        this.gameEventLoopMgr = gameEventLoopMgr;
        this.worldInfoMgr = worldInfoMgr;
        this.worldTimeMgr = worldTimeMgr;
        this.protocolDispatcherMgr = protocolDispatcherMgr;
        this.httpDispatcherMgr = httpDispatcherMgr;
        this.protocolCodecMgr = protocolCodecMgr;
        this.worldTimerMgr = worldTimerMgr;
        this.globalExecutorMgr = globalExecutorMgr;
        this.curatorMgr = curatorMgr;
        this.guidMgr = guidMgr;
        this.gameConfigMgr = gameConfigMgr;
        this.netContextMgr = netContextMgr;
        this.curatorClientMgr = curatorClientMgr;
        this.templateMgr = templateMgr;
        this.gameAcceptorMgr = gameAcceptorMgr;
        this.redisMgr = redisMgr;
    }

    public GameEventLoopMgr getGameEventLoopMgr() {
        return gameEventLoopMgr;
    }

    public WorldInfoMgr getWorldInfoMgr() {
        return worldInfoMgr;
    }

    public WorldTimeMgr getWorldTimeMgr() {
        return worldTimeMgr;
    }

    public ProtocolDispatcherMgr getProtocolDispatcherMgr() {
        return protocolDispatcherMgr;
    }

    public HttpDispatcherMgr getHttpDispatcherMgr() {
        return httpDispatcherMgr;
    }

    public ProtocolCodecMgr getProtocolCodecMgr() {
        return protocolCodecMgr;
    }

    public WorldTimerMgr getWorldTimerMgr() {
        return worldTimerMgr;
    }

    public GlobalExecutorMgr getGlobalExecutorMgr() {
        return globalExecutorMgr;
    }

    public CuratorMgr getCuratorMgr() {
        return curatorMgr;
    }

    public GuidMgr getGuidMgr() {
        return guidMgr;
    }

    public GameConfigMgr getGameConfigMgr() {
        return gameConfigMgr;
    }

    public NetContextMgr getNetContextMgr() {
        return netContextMgr;
    }

    public CuratorClientMgr getCuratorClientMgr() {
        return curatorClientMgr;
    }

    public TemplateMgr getTemplateMgr() {
        return templateMgr;
    }

    public GameAcceptorMgr getGameAcceptorMgr() {
        return gameAcceptorMgr;
    }

    public RedisMgr getRedisMgr() {
        return redisMgr;
    }
}
