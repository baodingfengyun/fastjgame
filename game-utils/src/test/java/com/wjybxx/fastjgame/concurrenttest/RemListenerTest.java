/*
 *
 *  * Copyright 2019 wjybxx
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.wjybxx.fastjgame.concurrenttest;

import com.wjybxx.fastjgame.concurrent.*;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/14
 * github - https://github.com/hl845740757
 */
public class RemListenerTest {

    public static void main(String[] args) {
        DefaultEventLoopGroup defaultEventLoopGroup = new DefaultEventLoopGroup(1, new DefaultThreadFactory("DEF"), RejectedExecutionHandlers.abort());
        DefaultPromise<Object> defaultPromise = new DefaultPromise<>(defaultEventLoopGroup.next());

        final FutureListener<Object> futureListener = future -> {
            System.out.println(future.getNow());
        };

        // debug追踪
        defaultPromise.addListener(futureListener);

        defaultPromise.removeListener(futureListener);

        System.out.println("");
    }

}
