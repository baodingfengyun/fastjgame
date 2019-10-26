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

package com.wjybxx.fastjgame.concurrent.event;

import com.wjybxx.fastjgame.eventbus.EventDispatcher;

import javax.annotation.Nonnull;

/**
 * 提交事件任务
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/10/26
 * github - https://github.com/hl845740757
 */
public class EventDispatchTask implements Runnable {

    private final EventDispatcher dispatcher;
    private final Object event;

    public EventDispatchTask(@Nonnull EventDispatcher dispatcher, @Nonnull Object event) {
        this.dispatcher = dispatcher;
        this.event = event;
    }

    @Override
    public void run() {
        dispatcher.post(event);
    }
}
