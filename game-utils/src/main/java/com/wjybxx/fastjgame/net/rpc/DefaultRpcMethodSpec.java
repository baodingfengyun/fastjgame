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

package com.wjybxx.fastjgame.net.rpc;


import com.wjybxx.fastjgame.net.binary.ObjectReader;
import com.wjybxx.fastjgame.net.binary.ObjectWriter;
import com.wjybxx.fastjgame.net.binary.PojoCodecImpl;
import com.wjybxx.fastjgame.net.serialization.TypeId;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.ArrayList;
import java.util.List;

/**
 * 默认的rpc方法结构体，可以根据{@link #serviceId} 和{@link #methodId}确定唯一的一个方法。
 * <p>
 * 警告：不要修改对象的内容，否则可能引发bug(并发错误)。
 *
 * @param <V> the type of return type
 * @author wjybxx
 * @version 1.1
 * date - 2019/8/19
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public class DefaultRpcMethodSpec<V> implements RpcMethodSpec<V> {

    /**
     * 远程服务id
     * 不使用 服务名 + 方法名 + 方法具体参数信息，传输的内容量过于庞大，性能不好。
     */
    private final short serviceId;
    /**
     * 远程方法id
     */
    private final short methodId;

    /**
     * 方法参数列表
     */
    private final List<Object> methodParams;

    /**
     * 需要延迟到网络层序列化为byte[]的参数位置信息。
     * 该参数不需要序列化，该对象在第一次序列化的时候，就将需要延迟初始化的参数完成了序列化。
     */
    private final int lazyIndexes;
    /**
     * 需要网络层提前反序列化的参数位置信息。
     * 需要序列化到接收方，真正的接收方在反序列化该对象的时候，才会使用到。
     */
    private final int preIndexes;

    public DefaultRpcMethodSpec(short serviceId, short methodId, List<Object> methodParams, int lazyIndexes, int preIndexes) {
        this.serviceId = serviceId;
        this.methodId = methodId;
        this.methodParams = methodParams;
        this.lazyIndexes = lazyIndexes;
        this.preIndexes = preIndexes;
    }

    public short getServiceId() {
        return serviceId;
    }

    public short getMethodId() {
        return methodId;
    }

    public List<Object> getMethodParams() {
        return methodParams;
    }

    public int getLazyIndexes() {
        return lazyIndexes;
    }

    public int getPreIndexes() {
        return preIndexes;
    }

    @Override
    public String toString() {
        return "DefaultRpcMethodSpec{" +
                "serviceId=" + serviceId +
                ", methodId=" + methodId +
                ", methodParams=" + methodParams +
                ", lazyIndexes=" + lazyIndexes +
                ", preIndexes=" + preIndexes +
                '}';
    }

    /**
     * 负责{@link DefaultRpcMethodSpec}的序列化工作 - 会被自动扫描到。
     */
    @SuppressWarnings({"unused"})
    private static class RpcMethodSpecCodec implements PojoCodecImpl<DefaultRpcMethodSpec<?>> {

        @SuppressWarnings("unchecked")
        @Override
        public Class<DefaultRpcMethodSpec<?>> getEncoderClass() {
            return (Class) DefaultRpcMethodSpec.class;
        }

        @Override
        public DefaultRpcMethodSpec<?> readObject(ObjectReader reader) throws Exception {
            final short serviceId = reader.readShort();
            final short methodId = reader.readShort();
            final int preIndexes = reader.readInt();

            final List<Object> methodParams = doPreDeserialize(reader, preIndexes);

            return new DefaultRpcMethodSpec<>(serviceId, methodId, methodParams, 0, 0);
        }

        private List<Object> doPreDeserialize(ObjectReader reader, int preIndexes) throws Exception {
            final ObjectReader.ReaderContext context = reader.readStartObject();
            // 方法参数一般小于等于4个，扩容一次6个也足够，超过6个参数的方法应该重构
            final List<Object> methodParams = new ArrayList<>(4);
            for (int index = 0; !reader.isEndOfObject(); index++) {
                final Object newParameter;
                if (preIndexes > 0 && (preIndexes & (1L << index)) != 0) {
                    newParameter = reader.readPreDeserializeObject();
                } else {
                    newParameter = reader.readObject();
                }
                methodParams.add(newParameter);
            }
            reader.readEndObject(context);
            return methodParams;
        }

        @Override
        public void writeObject(DefaultRpcMethodSpec<?> instance, ObjectWriter writer) throws Exception {
            writer.writeShort(instance.getServiceId());
            writer.writeShort(instance.getMethodId());
            writer.writeInt(instance.getPreIndexes());

            doLazySerialize(writer, instance.getMethodParams(), instance.getLazyIndexes());
        }

        private void doLazySerialize(ObjectWriter writer, final List<Object> methodParams, final int lazyIndexes) throws Exception {
            final ObjectWriter.WriterContext context = writer.writeStartObject(TypeId.DEFAULT_LIST);
            for (int index = 0, size = methodParams.size(); index < size; index++) {
                final Object parameter = methodParams.get(index);
                if (lazyIndexes > 0 && (lazyIndexes & (1L << index)) != 0) {
                    writer.writeLazySerializeObject(parameter);
                } else {
                    writer.writeObject(parameter);
                }
            }
            writer.writeEndObject(context);
        }
    }
}
