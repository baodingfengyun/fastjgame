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

package com.wjybxx.fastjgame.util.example;

import com.wjybxx.fastjgame.util.ThreadUtils;
import com.wjybxx.fastjgame.util.misc.StepWatch;

import java.util.concurrent.ThreadLocalRandom;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/11/28
 * github - https://github.com/hl845740757
 */
public class StepWatchExample {

    public static void main(String[] args) {
        final StepWatch stepWatch = new StepWatch("Example:loop");
        for (int index = 0; index < 10; index++) {
            stepWatch.restart();

            sleep();
            stepWatch.logStep("step1");

            sleep();
            stepWatch.logStep("step2");

            sleep();
            stepWatch.logStep("step3");

            sleep();
            stepWatch.logStep("step4");

            System.out.println(stepWatch.getLog());
        }
    }

    private static void sleep() {
        ThreadUtils.sleepQuietly(ThreadLocalRandom.current().nextInt(4, 10));
    }
}
