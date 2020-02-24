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

package com.wjybxx.fastjgame.net.example;

import com.wjybxx.fastjgame.net.binary.BinaryProtocolCodec;
import com.wjybxx.fastjgame.net.common.ProtocolCodec;
import com.wjybxx.fastjgame.net.misc.JsonProtocolCodec;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;

/**
 * 一个不太靠谱的完整编解码性能测试
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/21
 * github - https://github.com/hl845740757
 */
public class ProtocolCodecPerformanceTest {

    public static void main(String[] args) throws Exception {
        ByteBufAllocator byteBufAllocator = PooledByteBufAllocator.DEFAULT;
        ExampleMessages.FullMessage msg = BinaryProtoCodecTest.newFullMessage();
//        final TestMsg msg = new TestMsg(32116503156L, 5461166513213L, 546541211616512L, false);

        JsonProtocolCodec jsonCodec = ExampleConstants.jsonCodec;
        BinaryProtocolCodec binaryCodec = ExampleConstants.binaryCodec;

        // equals测试，正确性必须要保证
        equalsTest(jsonCodec, byteBufAllocator, msg);
        System.out.println();

        equalsTest(binaryCodec, byteBufAllocator, msg);
        System.out.println();

        // 预热
        codecTest(jsonCodec, byteBufAllocator, msg, 10_0000);
        codecTest(binaryCodec, byteBufAllocator, msg, 10_0000);
        System.out.println();

        // 开搞
        codecTest(jsonCodec, byteBufAllocator, msg, 100_0000);
        codecTest(binaryCodec, byteBufAllocator, msg, 100_0000);
    }

    private static void equalsTest(ProtocolCodec codec, ByteBufAllocator byteBufAllocator, Object msg) throws Exception {
        final String name = codec.getClass().getSimpleName();
        ByteBuf byteBuf = codec.writeObject(byteBufAllocator, msg);
        System.out.println(name + " encode result bytes = " + byteBuf.readableBytes());

        Object decodeMessage = codec.readObject(byteBuf);
        System.out.println(name + " codec equals result = " + msg.equals(decodeMessage));
        // 总是忘记release
        byteBuf.release();
    }

    private static void codecTest(ProtocolCodec codec, ByteBufAllocator byteBufAllocator, Object msg, int loopTimes) throws Exception {
        final String name = codec.getClass().getSimpleName();
        long start = System.currentTimeMillis();
        for (int index = 0; index < loopTimes; index++) {
            ByteBuf byteBuf = codec.writeObject(byteBufAllocator, msg);
            Object decodeMessage = codec.readObject(byteBuf);
            // 由于没真正发送，显式的进行释放
            byteBuf.release();
        }
        System.out.println(name + " codec " + loopTimes + " times cost timeMs " + (System.currentTimeMillis() - start));
    }
}
