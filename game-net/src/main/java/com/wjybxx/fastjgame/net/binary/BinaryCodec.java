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
import com.wjybxx.fastjgame.utils.entity.NumericalEntity;

import javax.annotation.Nonnull;

/**
 * 二进制编解码方案
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/17
 */
interface BinaryCodec<T> extends NumericalEntity {

    /**
     * 是否支持编码该对象
     *
     * @param runtimeType 运行时数据类型
     * @return 如果支持则返回true
     */
    boolean isSupport(Class<?> runtimeType);

    /**
     * 编码协议内容，不包含wireType
     *
     * @param outputStream 输出流
     * @param instance     待编码的对象
     */
    void writeData(CodedOutputStream outputStream, @Nonnull T instance) throws Exception;

    /**
     * 解码字段协议内容，不包含wireType
     *
     * @param inputStream 输入流
     * @return data，只有非null的tag才会走到这里
     */
    @Nonnull
    T readData(CodedInputStream inputStream) throws Exception;

    @Override
    default int getNumber() {
        return getWireType();
    }

    /**
     * 该codec对应的类型
     *
     * @return wireType
     */
    byte getWireType();

}
