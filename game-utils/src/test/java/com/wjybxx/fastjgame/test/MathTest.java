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

package com.wjybxx.fastjgame.test;

import com.wjybxx.fastjgame.misc.IntPair;
import com.wjybxx.fastjgame.utils.MathUtils;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2019/12/4
 * github - https://github.com/hl845740757
 */
public class MathTest {

    public static void main(String[] args) {
        System.out.println(MathUtils.divideIntCeil(4, 3));

        testCompose(1);
        testCompose(256);
        testCompose(65536);

        testCompose(-1);
        testCompose(-256);
        testCompose(-65536);
    }

    private static void testCompose(final int number) {
        testImp(number, -1);
        testImp(number, -256);
        testImp(number, -65536);

        testImp(-1, number);
        testImp(-256, number);
        testImp(-65536, number);
    }

    private static void testImp(int a, int b) {
        final long value = MathUtils.composeToLong(a, b);
        final IntPair pair = MathUtils.decomposeToInt(value);
        assert a == pair.getFirst() && b == pair.getSecond();

        System.out.println("\na = " + a + ", b = " + b + ", value " + value);
        System.out.println("first = " + pair.getFirst() + ", second " + pair.getSecond());
    }
}
