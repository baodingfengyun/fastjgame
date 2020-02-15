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

package com.wjybxx.fastjgame.utils.concurrent;

import com.wjybxx.fastjgame.utils.exception.InternalApiException;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.function.Supplier;

/**
 * EventLoop持有对象
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/10/9
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public class EventLoopHolder<T extends EventLoop> implements Supplier<T> {

    private T eventLoop;

    public final void publish(T eventLoop) {
        if (eventLoop.inEventLoop()) {
            // 保证线程安全性
            this.eventLoop = eventLoop;
        } else {
            throw new InternalApiException();
        }
    }

    public final T getEventLoop() {
        if (null == eventLoop) {
            throw new IllegalStateException();
        }
        return eventLoop;
    }

    @Override
    public T get() {
        return getEventLoop();
    }

    public final boolean inEventLoop() {
        if (null == eventLoop) {
            throw new IllegalStateException();
        }
        return eventLoop.inEventLoop();
    }
}
