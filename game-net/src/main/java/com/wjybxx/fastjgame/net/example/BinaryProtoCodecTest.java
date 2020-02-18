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
import com.wjybxx.fastjgame.net.utils.NetUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;

import java.util.*;

/**
 * 基于反射的编解码器的测试用例。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/19
 * github - https://github.com/hl845740757
 */
public class BinaryProtoCodecTest {

    public static void main(String[] args) throws Exception {
        BinaryProtocolCodec codec = ExampleConstants.binaryCodec;
        ByteBufAllocator byteBufAllocator = UnpooledByteBufAllocator.DEFAULT;
        // NetUtils初始化，避免输出扰乱视听
        System.out.println(NetUtils.getOuterIp());

        ExampleMessages.FullMessage fullMessage = newFullMessage();
        System.out.println(fullMessage);

        ByteBuf encodeResult = codec.writeObject(byteBufAllocator, fullMessage);

        Object decodeResult = codec.readObject(encodeResult);
        System.out.println(decodeResult);

        System.out.println("equals = " + fullMessage.equals(decodeResult));

        encodeResult.release();
    }

    /**
     * 一个正常赋值了的对象
     */
    public static ExampleMessages.FullMessage newFullMessage() {
        ExampleMessages.FullMessage fullMessage = new ExampleMessages.FullMessage();
        fullMessage.setAny("any");
        fullMessage.setaByte((byte) 25);
        fullMessage.setaChar('a');
        fullMessage.setaShort((short) 3222);
        fullMessage.setAnInt(6555895);
        fullMessage.setaLong(54654874561L);
        fullMessage.setaFloat(1.1f);
        fullMessage.setaDouble(2.0);
        fullMessage.setaBoolean(true);

        ExampleMessages.Hello hello = new ExampleMessages.Hello(65, "hello world.");
        fullMessage.setHello(hello);

        fullMessage.setaString("wjybxx");
        fullMessage.setProfession(ExampleMessages.Profession.CODER);

        fullMessage.setStringList(new ArrayList<>(Arrays.asList("张三", "李四", "王五")));
        fullMessage.setStringSet(new LinkedHashSet<>(Arrays.asList("zhangsan", "li", "wangwu")));

        Map<String, String> params = new LinkedHashMap<>();
        params.put("first", "abc");
        params.put("second", "def");
        fullMessage.setStringStringMap(params);

        fullMessage.setaByteArray(new byte[]{Byte.MIN_VALUE, 1, Byte.MAX_VALUE});
        fullMessage.setaShortArray(new short[]{Short.MIN_VALUE, 2, Short.MAX_VALUE});
        fullMessage.setaIntArray(new int[]{Integer.MIN_VALUE, 3, Integer.MAX_VALUE});
        fullMessage.setaLongArrray(new long[]{Long.MIN_VALUE, 4, Long.MAX_VALUE});
        fullMessage.setaFloatArray(new float[]{-5.5f, 0.1f, 5.5f});
        fullMessage.setaDoubleArray(new double[]{-6.6, 0.1f, 6.6});

        fullMessage.setaCharArray("hello world".toCharArray());

        return fullMessage;
    }

}
