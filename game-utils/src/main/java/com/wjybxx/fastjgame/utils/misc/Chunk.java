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

package com.wjybxx.fastjgame.utils.misc;

import org.apache.commons.lang3.ArrayUtils;

/**
 * 数据块
 * 序列化时，只序列化有效负荷部分
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/1/16
 * github - https://github.com/hl845740757
 */
public class Chunk {

    public static final Chunk EMPTY_CHUNK = newInstance(ArrayUtils.EMPTY_BYTE_ARRAY);

    private final byte[] buffer;

    private final int offset;

    private final int length;

    private Chunk(byte[] buffer, int offset, int length) {
        this.buffer = buffer;
        this.offset = offset;
        this.length = length;
    }

    public byte[] getBuffer() {
        return buffer;
    }

    public int getOffset() {
        return offset;
    }

    public int getLength() {
        return length;
    }

    public static Chunk newInstance(byte[] buffer) {
        return newInstance(buffer, 0, buffer.length);
    }

    public static Chunk newInstance(byte[] buffer, int offset, int length) {
        if (offset < 0 || length < 0 || (buffer.length - (offset + length)) < 0) {
            throw new IllegalArgumentException(String.format(
                    "Array range is invalid. Buffer.length=%d, offset=%d, length=%d",
                    buffer.length, offset, length));
        }
        return new Chunk(buffer, offset, length);
    }
}
