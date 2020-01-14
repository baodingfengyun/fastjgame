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

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.Message;
import com.google.protobuf.Parser;

import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * 这个编解码速度：
 * 100W次： 200 - 230ms
 * 确实很快！但是在服务器内部之间，使用protobuf的话也很有限制，比如包装类型，复杂数据结构(Map)。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/1/14
 * github - https://github.com/hl845740757
 */
public class ProtoBufSerializePerformanceTest {

    private static final byte[] buffer = new byte[8192];
    private static final int TEST_MSG_HASH_CODE = ExampleHashMappingStrategy.getUniqueId(TestMsg.class);

    public static void main(String[] args) throws IOException {
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
    }

    private static void equalsTest(p_test.p_testMsg msg) throws IOException {
        final Parser<p_test.p_testMsg> parser = msg.getParserForType();

        // 手动写入一个messageId
        final CodedOutputStream codedOutputStream = CodedOutputStream.newInstance(buffer);
        codedOutputStream.writeSInt32NoTag(TEST_MSG_HASH_CODE);
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
            codedOutputStream.writeSInt32NoTag(TEST_MSG_HASH_CODE);
            msg.writeTo(codedOutputStream);

            // 这里需要简单模拟下查询
            final Parser<?> parser = parserMap.get(msg.getClass());
            final CodedInputStream inputStream = CodedInputStream.newInstance(buffer, 0, codedOutputStream.getTotalBytesWritten());
            inputStream.readSInt32();
            final Object decodeMsg = parser.parseFrom(inputStream);
        }
        System.out.println(" codec " + loopTimes + " times cost timeMs " + (System.currentTimeMillis() - start));
    }
}
