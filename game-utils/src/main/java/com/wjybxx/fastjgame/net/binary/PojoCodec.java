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
interface PojoCodec<T> {

    /**
     * 获取负责编解码的类对象
     */
    Class<T> getEncoderClass();

    /**
     * 从输入流中解析指定对象。
     * 它应该创建对象，并反序列化该类及其所有超类定义的所有要序列化的字段。
     */
    T readObject(DataInputStream dataInputStream, CodecRegistry codecRegistry, ObjectReader reader) throws Exception;

    /**
     * 将对象写入输出流。
     * 将对象及其所有超类定义的所有要序列化的字段写入输出流。
     */
    void writeObject(T instance, DataOutputStream dataOutputStream, CodecRegistry codecRegistry, ObjectWriter writer) throws Exception;
}
