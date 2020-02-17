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

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 * 普通JavaBean对象输出流。
 * Q: 为什么必须使用包装类型？
 * A: 某些时刻需要使用null表示未赋值状态，使用特殊值是不好的。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/1/13
 * github - https://github.com/hl845740757
 */
public interface EntityOutputStream {

    /**
     * 向输入流中写入一个字段
     * (方便手写实现)
     *
     * @param fieldValue 字段的值
     */
    default <T> void writeField(@Nullable T fieldValue) throws IOException {
        writeField(WireType.RUN_TIME, fieldValue);
    }

    /**
     * 向输入流中写入一个字段
     * (给生成代码使用的)。
     *
     * @param wireType   字段的缓存类型，如果该值为{@link WireType#RUN_TIME}，则需要动态解析。
     * @param fieldValue 字段的值
     */
    <T> void writeField(byte wireType, @Nullable T fieldValue) throws IOException;

    /**
     * 向输入流中写入一个map
     */
    <K, V> void writeMap(@Nullable Map<K, V> map) throws IOException;

    /**
     * 像输入流中写一个collection
     */
    <E> void writeCollection(@Nullable Collection<E> collection) throws IOException;
}
