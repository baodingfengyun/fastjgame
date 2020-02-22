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

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;

import javax.annotation.Nonnull;

/**
 * 基础类型编解码器
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/22
 * github - https://github.com/hl845740757
 */
public interface PrimitiveCodec<T, U> extends BinaryCodec<T> {

    /**
     * 写入数组的内容
     * 注意：数组的长度已经写入
     */
    void writeArray(CodedOutputStream outputStream, @Nonnull U array) throws Exception;

    /**
     * 读取指定长度的数组
     */
    U readArray(CodedInputStream inputStream, int length) throws Exception;

}
