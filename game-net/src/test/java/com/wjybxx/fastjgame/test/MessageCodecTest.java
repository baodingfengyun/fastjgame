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

import com.wjybxx.fastjgame.net.binary.BinaryProtocolCodec;
import com.wjybxx.fastjgame.net.example.ExampleConstants;
import com.wjybxx.fastjgame.net.example.p_test;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;

import java.util.Arrays;

/**
 * 测试基于反射调用的编解码解码protobuf消息
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/9/10
 * github - https://github.com/hl845740757
 */
public class MessageCodecTest {

    public static void main(String[] args) throws Exception {
        BinaryProtocolCodec codec = ExampleConstants.binaryCodec;
        ByteBufAllocator byteBufAllocator = UnpooledByteBufAllocator.DEFAULT;

        final p_test.p_helloworld hello = p_test.p_helloworld.newBuilder()
                .setA(1)
                .setB(5506665554142L)
                .addAllC(Arrays.asList(0, 1, 2, 3, 4, 5))
                .setE("hello")
                .setF(true)
                .setG(1.1f)
                .setH(2.0d)
                .setK(p_test.ERole.AGE)
                .build();

        ByteBuf encodeResult = codec.writeObject(byteBufAllocator, hello);

        Object decodeResult = codec.readObject(encodeResult);
        System.out.println(decodeResult);

        System.out.println("equals = " + hello.equals(decodeResult));

        encodeResult.release();
    }
}
