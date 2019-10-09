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
import com.wjybxx.fastjgame.timer.SystemTimeHelper;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * World级别系统时间控制器，非线程安全。
 * 目的为了减少频繁地调用{@link System#currentTimeMillis()}
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/4/27 22:06
 * github - https://github.com/hl845740757
 */
@EventLoopSingleton
@NotThreadSafe
public class WorldTimeMgr extends SystemTimeHelper {

    @Inject
    public WorldTimeMgr() {

    }
}
