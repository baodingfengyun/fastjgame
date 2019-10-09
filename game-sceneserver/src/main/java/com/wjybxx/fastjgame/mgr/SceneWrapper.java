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

/**
 * 封装scene需要的控制器
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/6/4 19:33
 * github - https://github.com/hl845740757
 */
public class SceneWrapper {

    private final SceneSendMgr sendMrg;
    private final MapDataLoadMgr mapDataLoadMgr;
    private final WorldTimeMgr worldTimeMgr;
    private final WorldTimerMgr worldTimerMgr;
    private final GuidMgr guidMgr;

    @Inject
    public SceneWrapper(SceneSendMgr sendMrg, MapDataLoadMgr mapDataLoadMgr, WorldTimeMgr worldTimeMgr, WorldTimerMgr worldTimerMgr, GuidMgr guidMgr) {
        this.sendMrg = sendMrg;
        this.mapDataLoadMgr = mapDataLoadMgr;
        this.worldTimeMgr = worldTimeMgr;
        this.worldTimerMgr = worldTimerMgr;
        this.guidMgr = guidMgr;
    }

    public SceneSendMgr getSendMrg() {
        return sendMrg;
    }

    public MapDataLoadMgr getMapDataLoadMgr() {
        return mapDataLoadMgr;
    }

    public WorldTimeMgr getWorldTimeMgr() {
        return worldTimeMgr;
    }

    public WorldTimerMgr getWorldTimerMgr() {
        return worldTimerMgr;
    }

    public GuidMgr getGuidMgr() {
        return guidMgr;
    }
}
