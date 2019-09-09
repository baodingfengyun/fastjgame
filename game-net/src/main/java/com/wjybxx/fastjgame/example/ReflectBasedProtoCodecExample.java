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
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;

import java.io.IOException;
import java.util.*;

/**
 * 基于反射的编解码器的测试用例。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/19
 * github - https://github.com/hl845740757
 */
public class ReflectBasedProtoCodecExample {

    public static void main(String[] args) throws IOException {
        ExampleMessages.FullMessage fullMessage = getFullMessage();
        System.out.println(fullMessage);

        ReflectBasedProtocolCodec codec = ExampleConstants.reflectBasedCodec;
        ByteBufAllocator byteBufAllocator = UnpooledByteBufAllocator.DEFAULT;
        ByteBuf encodeResult = codec.encodeMessage(byteBufAllocator, fullMessage);

        Object decodeResult = codec.decodeMessage(encodeResult);
        System.out.println(decodeResult);

        System.out.println("equals = " + fullMessage.equals(decodeResult));

        encodeResult.release();
    }

    /**
     * 一个正常赋值了的对象
     */
    static ExampleMessages.FullMessage getFullMessage() {
        ExampleMessages.FullMessage fullMessage = new ExampleMessages.FullMessage();
        fullMessage.setaByte((byte) 25);
        fullMessage.setaChar('a');
        fullMessage.setaShort((short) 3222);
        fullMessage.setAnInt(6555895);
        fullMessage.setaLong(54654874561L);
        fullMessage.setaFloat(1.1f);
        fullMessage.setaDouble(2.0);
        fullMessage.setaBoolean(true);

        ExampleMessages.Hello hello = new ExampleMessages.Hello();
        hello.setId(65);
        hello.setMessage("hello world.");
        fullMessage.setHello(hello);

        fullMessage.setName("wjybxx");
        fullMessage.setProfession(ExampleMessages.Profession.CODER);

        fullMessage.setStringList(new ArrayList<>(Arrays.asList("张三", "lisi", "55")));
        fullMessage.setStringSet(new LinkedHashSet<>(Arrays.asList("张三", "lisi", "55")));

        Map<String, String> params = new LinkedHashMap<>();
        params.put("first", "abc");
        params.put("second", "def");
        fullMessage.setStringStringMap(params);

        return fullMessage;
    }

}
