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

package com.wjybxx.fastjgame.misc;

import com.wjybxx.fastjgame.net.Session;
import com.wjybxx.fastjgame.net.SessionLifecycleAware;

/**
 * 连接建立时的通知任务 - 消lambda表达式
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/9/18
 * github - https://github.com/hl845740757
 */
public class ConnectAwareTask implements Runnable{

    private final Session session;
    private final SessionLifecycleAware lifecycleAware;

    public ConnectAwareTask(Session session, SessionLifecycleAware lifecycleAware) {
        this.session = session;
        this.lifecycleAware = lifecycleAware;
    }

    @Override
    public void run() {
        lifecycleAware.onSessionConnected(session);
    }
}
