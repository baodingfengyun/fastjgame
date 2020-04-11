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

package com.wjybxx.fastjgame.concurrenttest;

import com.wjybxx.fastjgame.utils.concurrent.EventLoop;
import com.wjybxx.fastjgame.utils.concurrent.GlobalEventLoop;
import com.wjybxx.fastjgame.utils.concurrent.GuardedOperationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/4/11
 * github - https://github.com/hl845740757
 */
public class LocalFutureTest {

    @Test
    void testGlobalEventLoop() {
        Assertions.assertThrows(GuardedOperationException.class, () -> addListener(GlobalEventLoop.INSTANCE));
    }

    private void addListener(EventLoop eventLoop) {
        eventLoop.newLocalPromise()
                .addListener(f -> System.out.println(f.getNow()));
    }

}
