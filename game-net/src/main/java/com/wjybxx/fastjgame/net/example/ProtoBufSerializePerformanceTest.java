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

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.Message;
import com.google.protobuf.Parser;
import com.wjybxx.fastjgame.net.misc.HashMessageMappingStrategy;
import com.wjybxx.fastjgame.net.misc.MessageMapper;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * 一个不太靠谱的protoBuf编解码测试。
 * 编解码速度：
 * 100W次： 170ms左右(新电脑)
 * 确实很快！但是在服务器内部之间，使用protobuf的话也很有限制，比如包装类型，复杂数据结构(Map)，null等。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/1/14
 * github - https://github.com/hl845740757
 */
public class ProtoBufSerializePerformanceTest {

    private static final byte[] buffer = new byte[8192];
    private static final MessageMapper messageMapper = MessageMapper.newInstance(() -> {
        final Object2IntMap<Class<?>> result = new Object2IntOpenHashMap<>();
        result.put(p_test.p_testMsg.class, HashMessageMappingStrategy.getUniqueId(TestMsg.class));
        return result;
    });

    public static void main(String[] args) throws IOException, InterruptedException {
        // 默认值不会被序列化(0, false)，因此我们要避免默认值
        final p_test.p_testMsg msg = p_test.p_testMsg.newBuilder()
                .setSceneId(32116503156L)
                .setFactionId(5461166513213L)
                .setOwnerId(546541211616512L)
                .setOwnerSupportAR(true)
                .setPlayerNum(1)
                .setRacing(true)
                .build();

        System.out.println(msg.toString());
        equalsTest(msg);

        Map<Class<?>, Parser<? extends Message>> parserMap = new IdentityHashMap<>();
        parserMap.put(msg.getClass(), msg.getParserForType());
        System.out.println();

        // 预热
        codecTest(msg, 10_0000, parserMap);

        // 开跑
        codecTest(msg, 100_0000, parserMap);

        Thread.sleep(1000);
    }

    private static void equalsTest(p_test.p_testMsg msg) throws IOException {
        final Parser<p_test.p_testMsg> parser = msg.getParserForType();

        final CodedOutputStream codedOutputStream = CodedOutputStream.newInstance(buffer);
        codedOutputStream.writeSInt32NoTag(messageMapper.getMessageId(msg.getClass()));
        msg.writeTo(codedOutputStream);
        System.out.println(" encode result bytes = " + codedOutputStream.getTotalBytesWritten());

        final CodedInputStream inputStream = CodedInputStream.newInstance(buffer, 0, codedOutputStream.getTotalBytesWritten());
        inputStream.readSInt32();
        final Object decodeMsg = parser.parseFrom(inputStream);
        System.out.println(" codec equals result = " + msg.equals(decodeMsg));
    }

    private static void codecTest(Message msg, int loopTimes, Map<Class<?>, Parser<? extends Message>> parserMap) throws IOException {
        final long start = System.currentTimeMillis();
        for (int index = 0; index < loopTimes; index++) {
            final CodedOutputStream codedOutputStream = CodedOutputStream.newInstance(buffer);
            codedOutputStream.writeSInt32NoTag(messageMapper.getMessageId(msg.getClass()));
            msg.writeTo(codedOutputStream);

            // 这里需要简单模拟下解码过程
            final CodedInputStream inputStream = CodedInputStream.newInstance(buffer, 0, codedOutputStream.getTotalBytesWritten());
            final int messageId = inputStream.readSInt32();
            final Class<?> messageClass = messageMapper.getMessageClazz(messageId);
            final Parser<?> parser = parserMap.get(messageClass);
            final Object decodeMsg = parser.parseFrom(inputStream);
        }
        System.out.println(" codec " + loopTimes + " times cost timeMs " + (System.currentTimeMillis() - start));
    }
}
