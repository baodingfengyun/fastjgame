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

package com.wjybxx.fastjgame.net.binary;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.Internal;
import com.google.protobuf.ProtocolMessageEnum;
import com.wjybxx.fastjgame.net.misc.MessageMapper;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Map;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/17
 */
class ProtoEnumCodec implements BinaryCodec<ProtocolMessageEnum> {

    /**
     * 实体映射
     */
    private final MessageMapper messageMapper;
    /**
     * proto enum 解析方法
     */
    private final Map<Class<?>, ProtoEnumDescriptor> protoEnumDescriptorMap;

    ProtoEnumCodec(MessageMapper messageMapper, Map<Class<?>, ProtoEnumDescriptor> protoEnumDescriptorMap) {
        this.messageMapper = messageMapper;
        this.protoEnumDescriptorMap = protoEnumDescriptorMap;
    }

    @Override
    public boolean isSupport(Class<?> runtimeType) {
        return protoEnumDescriptorMap.containsKey(runtimeType);
    }

    @Override
    public void writeData(CodedOutputStream outputStream, @Nonnull ProtocolMessageEnum instance) throws IOException {
        outputStream.writeSInt32NoTag(messageMapper.getMessageId(instance.getClass()));
        outputStream.writeEnumNoTag(instance.getNumber());
    }

    @Nonnull
    @Override
    public ProtocolMessageEnum readData(CodedInputStream inputStream) throws IOException {
        final int messageId = inputStream.readSInt32();
        final int number = inputStream.readEnum();
        final Class<?> enumClass = messageMapper.getMessageClazz(messageId);
        try {
            return (ProtocolMessageEnum) protoEnumDescriptorMap.get(enumClass).mapper.findValueByNumber(number);
        } catch (Exception e) {
            throw new IOException(enumClass.getName(), e);
        }
    }

    @Override
    public byte getWireType() {
        return WireType.PROTO_ENUM;
    }


    static class ProtoEnumDescriptor {

        private final Internal.EnumLiteMap<?> mapper;

        ProtoEnumDescriptor(Internal.EnumLiteMap<?> mapper) {
            this.mapper = mapper;
        }
    }
}
