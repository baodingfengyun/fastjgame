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

package com.wjybxx.fastjgame.example;

import com.wjybxx.fastjgame.misc.BinaryProtocolCodec;
import com.wjybxx.fastjgame.misc.JsonProtocolCodec;
import com.wjybxx.fastjgame.net.common.ProtocolCodec;

import java.io.IOException;

/**
 * 一个不太靠谱的序列化反序列化性能测试。
 * {@link TestMsg}测试结果大概是这样：
 * 旧版基于反射100W次编解码： 490 - 510 ms
 * 新版基于生成的代码100W次编解码： 310 - 330 ms
 * 差距还是有的，当然这里并不一定是瓶颈，但是游戏IO占的比重还是蛮大的，优化它还是有意义的。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/10/10
 * github - https://github.com/hl845740757
 */
public class SerializePerformanceTest {

    public static void main(String[] args) throws IOException, InterruptedException {
//        ExampleMessages.FullMessage msg = BinaryProtoCodecTest.newFullMessage();
        final TestMsg msg = new TestMsg(32116503156L, 5461166513213L, 546541211616512L, false);

        JsonProtocolCodec jsonCodec = ExampleConstants.jsonCodec;
        BinaryProtocolCodec binaryCodec = ExampleConstants.binaryCodec;

        // equals测试，正确性必须要保证
        equalsTest(jsonCodec, msg);
        System.out.println();

        equalsTest(binaryCodec, msg);
        System.out.println();

        // 预热
        codecTest(jsonCodec, msg, 10_0000);
        codecTest(binaryCodec, msg, 10_0000);
        System.out.println();

        // 开搞
        codecTest(jsonCodec, msg, 100_0000);
        codecTest(binaryCodec, msg, 100_0000);

        Thread.sleep(1000);
    }

    private static void equalsTest(ProtocolCodec codec, Object msg) throws IOException {
        final String name = codec.getClass().getSimpleName();
        final byte[] bytes = codec.serializeToBytes(msg);
        System.out.println(name + " encode result bytes = " + bytes.length);

        final Object decodeMessage = codec.deserializeFromBytes(bytes);
        System.out.println(name + " codec equals result = " + msg.equals(decodeMessage));
    }

    private static void codecTest(ProtocolCodec codec, Object msg, int loopTimes) throws IOException {
        final String name = codec.getClass().getSimpleName();
        final long start = System.currentTimeMillis();
        for (int index = 0; index < loopTimes; index++) {
            byte[] byteBuf = codec.serializeToBytes(msg);
            Object decodeMessage = codec.deserializeFromBytes(byteBuf);
        }
        System.out.println(name + " codec " + loopTimes + " times cost timeMs " + (System.currentTimeMillis() - start));
    }
}
