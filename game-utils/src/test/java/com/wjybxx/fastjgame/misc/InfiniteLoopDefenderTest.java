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

import com.wjybxx.fastjgame.util.exception.InfiniteLoopException;
import com.wjybxx.fastjgame.util.misc.InfiniteLoopDefender;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

/**
 * @author wjybxx
 * date - 2020/11/3
 * github - https://github.com/hl845740757
 */
class InfiniteLoopDefenderTest {

    private static final int LOOP_LIMIT = 1000;

    @Test
    void safe() {
        final InfiniteLoopDefender infiniteLoopDefender = new InfiniteLoopDefender(LOOP_LIMIT);
        IntStream.rangeClosed(1, LOOP_LIMIT)
                .forEach(i -> infiniteLoopDefender.check());
    }

    @Test
    void unsafe() {
        Assertions.assertThrows(InfiniteLoopException.class, () -> {
            final InfiniteLoopDefender infiniteLoopDefender = new InfiniteLoopDefender(LOOP_LIMIT);
            IntStream.rangeClosed(1, LOOP_LIMIT + 1)
                    .forEach(i -> infiniteLoopDefender.check());
        });
    }
}
