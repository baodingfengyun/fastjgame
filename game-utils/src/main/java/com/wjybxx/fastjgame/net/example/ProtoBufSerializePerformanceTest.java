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

import com.google.protobuf.*;
import com.wjybxx.fastjgame.net.binary.BinaryValueType;
import com.wjybxx.fastjgame.net.serialization.DefaultTypeIdMapper;
import com.wjybxx.fastjgame.net.serialization.TypeId;
import com.wjybxx.fastjgame.net.serialization.TypeIdMapper;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;
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

    private static final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(8192);
    private static final ExtensionRegistryLite emptyRegistry = ExtensionRegistryLite.getEmptyRegistry();

    private static final TypeIdMapper TYPE_ID_MAPPER = DefaultTypeIdMapper.newInstance(
            Collections.singleton(p_test.p_testMsg.class), ExampleConstants.typeIdMappingStrategy);

    /**
     * 默认值不会被序列化(0, false)，因此我们要避免默认值
     */
    static final p_test.p_testMsg msg = p_test.p_testMsg.newBuilder()
            .setSceneId(32116503156L)
            .setFactionId(5461166513213L)
            .setOwnerId(546541211616512L)
            .setOwnerSupportAR(true)
            .setPlayerNum(1)
            .setRacing(true)
            .build();

    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println(msg.toString());

        Map<Class<?>, Parser<? extends Message>> parserMap = new IdentityHashMap<>();
        parserMap.put(msg.getClass(), msg.getParserForType());

        equalsTest(msg, parserMap);

        System.out.println();

        // 预热
        codecTest(msg, 10_0000, parserMap);

        // 开跑
        codecTest(msg, 100_0000, parserMap);

        Thread.sleep(1000);
    }

    private static void equalsTest(MessageLite msg, Map<Class<?>, Parser<? extends Message>> parserMap) throws IOException {
        final CodedOutputStream codedOutputStream = encode(msg);
        System.out.println("encode result bytes = " + codedOutputStream.getTotalBytesWritten());

        byteBuffer.flip();

        final Object decodeMsg = decode(parserMap);
        System.out.println("codec equals result = " + msg.equals(decodeMsg));

        byteBuffer.clear();
    }

    private static CodedOutputStream encode(MessageLite msg) throws IOException {
        final CodedOutputStream codedOutputStream = CodedOutputStream.newInstance(byteBuffer);
        codedOutputStream.writeRawByte(BinaryValueType.OBJECT.getNumber());
        codedOutputStream.writeFixed32NoTag(0);

        writeTypeId(msg, codedOutputStream);
        codedOutputStream.writeMessageNoTag(msg);

        codedOutputStream.flush();
        return codedOutputStream;
    }

    private static void writeTypeId(Object msg, CodedOutputStream codedOutputStream) throws IOException {
        final TypeId typeId = TYPE_ID_MAPPER.ofType(msg.getClass());
        assert null != typeId;
        codedOutputStream.writeRawByte(typeId.getNamespace());
        codedOutputStream.writeFixed32NoTag(typeId.getClassId());
    }

    private static Object decode(Map<Class<?>, Parser<? extends Message>> parserMap) throws IOException {
        final CodedInputStream inputStream = CodedInputStream.newInstance(byteBuffer);
        final BinaryValueType valueType = BinaryValueType.forNumber(inputStream.readRawByte());
        final int length = inputStream.readFixed32();

        final Class<?> messageClass = readType(inputStream);
        final Parser<? extends Message> parser = parserMap.get(messageClass);
        return inputStream.readMessage(parser, emptyRegistry);
    }

    private static Class<?> readType(CodedInputStream inputStream) throws IOException {
        final byte nameSpace = inputStream.readRawByte();
        final int classId = inputStream.readFixed32();
        final Class<?> type = TYPE_ID_MAPPER.ofId(new TypeId(nameSpace, classId));
        assert null != type;
        return type;
    }

    private static void codecTest(Message msg, int loopTimes, Map<Class<?>, Parser<? extends Message>> parserMap) throws IOException {
        final long start = System.currentTimeMillis();
        for (int index = 0; index < loopTimes; index++) {
            encode(msg);

            byteBuffer.flip();

            decode(parserMap);

            byteBuffer.clear();
        }
        System.out.println("codec " + loopTimes + " times cost timeMs " + (System.currentTimeMillis() - start));
    }
}
