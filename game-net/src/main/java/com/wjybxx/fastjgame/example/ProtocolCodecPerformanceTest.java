/*
 *    Copyright 2019 wjybxx
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.wjybxx.fastjgame.example;

import com.wjybxx.fastjgame.misc.ReflectBasedProtocolCodec;
import com.wjybxx.fastjgame.net.common.ProtocolCodec;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;

import java.io.IOException;

/**
 * 一个不太标准的序列化性能测试
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/21
 * github - https://github.com/hl845740757
 */
public class ProtocolCodecPerformanceTest {

    public static void main(String[] args) throws IOException {
        ByteBufAllocator byteBufAllocator = PooledByteBufAllocator.DEFAULT;
//        ExampleMessages.FullMessage msg = ReflectBasedProtoCodecTest.newFullMessage();

//        final ExampleMessages.Hello msg = new ExampleMessages.Hello();
//        msg.setId(456456161613216L);
////        msg.setMessage("165LKMASNDKLAASNCLKALAS");

        final TestMsg msg = new TestMsg(32116503156L, 5461166513213L, 546541211616512L, false);

//        JsonBasedProtocolCodec jsonBasedCodec = ExampleConstants.jsonBasedCodec;
        ReflectBasedProtocolCodec reflectBasedCodec = ExampleConstants.reflectBasedCodec;

        // equals测试，正确性必须要保证
//        equalsTest(jsonBasedCodec, byteBufAllocator, msg);
//        System.out.println();

        equalsTest(reflectBasedCodec, byteBufAllocator, msg);
//        System.out.println();

        // 预热
//        codecTest(jsonBasedCodec, byteBufAllocator, msg, 1000);
        codecTest(reflectBasedCodec, byteBufAllocator, msg, 10_0000);
//        System.out.println();

        // 开搞
//        codecTest(jsonBasedCodec, byteBufAllocator, msg, 10_0000);
        codecTest(reflectBasedCodec, byteBufAllocator, msg, 100_0000);
    }

    private static void equalsTest(ProtocolCodec codec, ByteBufAllocator byteBufAllocator, Object msg) throws IOException {
        final String name = codec.getClass().getSimpleName();
        ByteBuf byteBuf = codec.writeObject(byteBufAllocator, msg);
        System.out.println(name + " encode result bytes = " + byteBuf.readableBytes());

        Object decodeMessage = codec.readObject(byteBuf);
        System.out.println(name + " codec equals result = " + msg.equals(decodeMessage));
        // 总是忘记release
        byteBuf.release();
    }

    private static void codecTest(ProtocolCodec codec, ByteBufAllocator byteBufAllocator, Object msg, int loopTimes) throws IOException {
        long start = System.currentTimeMillis();
        for (int index = 0; index < loopTimes; index++) {
            byte[] byteBuf = codec.serializeToBytes(msg);
            Object decodeMessage = codec.deserializeFromBytes(byteBuf);
        }
        System.out.println(" codec " + loopTimes + " times cost timeMs " + (System.currentTimeMillis() - start));
    }
}
