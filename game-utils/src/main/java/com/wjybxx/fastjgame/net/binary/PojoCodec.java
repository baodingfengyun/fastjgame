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

package com.wjybxx.fastjgame.net.binary;

/**
 * POJO编解码内部实现
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/8/1
 * github - https://github.com/hl845740757
 */
public final class PojoCodec<T> {

    private PojoCodecImpl<T> codec;

    PojoCodec(PojoCodecImpl<T> codec) {
        this.codec = codec;
    }

    /**
     * 获取负责编解码的类对象
     */
    public Class<T> getEncoderClass() {
        return codec.getEncoderClass();
    }

    /**
     * 从输入流中解析指定对象。
     * 它应该创建对象，并反序列化该类及其所有超类定义的所有要序列化的字段。
     */
    public T readObject(ObjectReader reader) throws Exception {
        return codec.readObject(reader);
    }

    /**
     * 将对象写入输出流。
     * 将对象及其所有超类定义的所有要序列化的字段写入输出流。
     */
    public void writeObject(ObjectWriter writer, T instance) throws Exception {
        codec.writeObject(instance, writer);
    }

    /**
     * 是否支持读取字段方法
     */
    public boolean isReadFieldsSupported() {
        return codec instanceof AbstractPojoCodecImpl;
    }

    public void readFields(ObjectReader reader, T instance) throws Exception {
        final AbstractPojoCodecImpl<T> abstractPojoCodec = (AbstractPojoCodecImpl<T>) codec;
        abstractPojoCodec.readFields(instance, reader);
    }

}
