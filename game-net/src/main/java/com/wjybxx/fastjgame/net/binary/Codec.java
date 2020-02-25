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

import javax.annotation.Nonnull;

/**
 * 编解码器，一个编解码器负责编码一个确定的类型。
 * 注意：实现必须是无状态的或不可变对象，以免产生并发错误。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/24
 */
public interface Codec<T> {

    /**
     * 将对象写入输出流
     *
     * @param codecRegistry 如果需要编解码别的类，可以获取对应的codec，但默认建议使用{@link EntityOutputStream#writeObject(Object)}方法
     */
    void encode(@Nonnull CodedOutputStream outputStream, @Nonnull T value, CodecRegistry codecRegistry) throws Exception;

    /**
     * 从输入流中读取对象
     *
     * @param codecRegistry 如果需要编解码别的类，可以获取对应的codec，但默认建议使用{@link EntityInputStream#readObject()}方法
     */
    @Nonnull
    T decode(@Nonnull CodedInputStream inputStream, CodecRegistry codecRegistry) throws Exception;

    /**
     * 获取代表这个codec的class对象
     */
    Class<?> getEncoderClass();
}
