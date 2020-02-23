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
import com.wjybxx.fastjgame.net.misc.MessageMapper;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Map;

/**
 * 自定义实体对象 - 通过生成的辅助类或手写的{@link EntitySerializer}进行编解码
 */
class CustomEntityCodec implements BinaryCodec<Object> {

    private final MessageMapper messageMapper;
    private final Map<Class<?>, EntitySerializer<?>> beanSerializerMap;
    private final BinaryProtocolCodec binaryProtocolCodec;

    CustomEntityCodec(MessageMapper messageMapper,
                      Map<Class<?>, EntitySerializer<?>> beanSerializerMap,
                      BinaryProtocolCodec binaryProtocolCodec) {
        this.messageMapper = messageMapper;
        this.beanSerializerMap = beanSerializerMap;
        this.binaryProtocolCodec = binaryProtocolCodec;
    }

    @Override
    public boolean isSupport(Class<?> runtimeType) {
        return beanSerializerMap.containsKey(runtimeType);
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public void writeDataNoTag(CodedOutputStream outputStream, @Nonnull Object instance) throws Exception {
        final Class<?> messageClass = instance.getClass();
        outputStream.writeInt32NoTag(messageMapper.getMessageId(messageClass));

        final EntitySerializer entitySerializer = beanSerializerMap.get(messageClass);
        final EntityOutputStreamImp beanOutputStreamImp = new EntityOutputStreamImp(binaryProtocolCodec, messageMapper, outputStream);

        try {
            entitySerializer.writeObject(instance, beanOutputStreamImp);
        } catch (Exception e) {
            throw new IOException(messageClass.getName(), e);
        }
    }

    @Nonnull
    @Override
    public Object readData(CodedInputStream inputStream) throws Exception {
        final int messageId = inputStream.readInt32();
        final Class<?> messageClass = messageMapper.getMessageClass(messageId);

        if (null == messageClass) {
            throw new IOException("unknown message id " + messageId);
        }

        final EntitySerializer<?> entitySerializer = beanSerializerMap.get(messageClass);
        final EntityInputStreamImp beanInputStreamImp = new EntityInputStreamImp(binaryProtocolCodec, messageMapper, inputStream);

        try {
            return entitySerializer.readObject(beanInputStreamImp);
        } catch (Exception e) {
            throw new IOException(messageClass.getName(), e);
        }
    }

    @Override
    public byte getWireType() {
        return WireType.CUSTOM_ENTITY;
    }

}
