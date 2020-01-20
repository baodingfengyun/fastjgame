/*
 *  Copyright 2019 wjybxx
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to iBn writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.wjybxx.fastjgame.mgr;

import com.google.inject.Inject;
import com.wjybxx.fastjgame.annotation.EventLoopSingleton;
import com.wjybxx.fastjgame.eventbus.SpecialEventBus;
import com.wjybxx.fastjgame.misc.GenericWorldEvent;
import com.wjybxx.fastjgame.misc.WorldEvent;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * World范围内使用的事件分发器
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/12/27
 * github - https://github.com/hl845740757
 */
@EventLoopSingleton
@NotThreadSafe
public class WorldEventMgr extends SpecialEventBus<WorldEvent, GenericWorldEvent<?>> {

    @Inject
    public WorldEventMgr() {
        super(1024);
    }

}
