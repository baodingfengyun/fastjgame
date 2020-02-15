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

import com.wjybxx.fastjgame.utils.concurrent.GlobalEventLoop;

public class GlobalEventLoopTest {

    public static void main(String[] args) throws InterruptedException {
        GlobalEventLoop globalEventLoop = GlobalEventLoop.INSTANCE;

        for (int index = 0; index < 10; index++) {
            globalEventLoop.submit(() -> {
                System.out.println("thread " + Thread.currentThread().getName() + ", task - inEventLoop = " + globalEventLoop.inEventLoop());
            });

            System.out.println("thread " + Thread.currentThread().getName() + ", main - inEventLoop = " + globalEventLoop.inEventLoop());

            // 睡眠5秒，等待GlobalEvent关闭
            Thread.sleep(5000);
        }
    }
}
