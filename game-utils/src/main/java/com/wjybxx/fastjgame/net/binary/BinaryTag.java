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


import com.wjybxx.fastjgame.net.type.TypeId;
import com.wjybxx.fastjgame.utils.EnumUtils;
import com.wjybxx.fastjgame.utils.dsl.IndexableEnum;
import com.wjybxx.fastjgame.utils.dsl.IndexableEnumMapper;

import java.util.Collection;
import java.util.Map;

/**
 * 数据类型
 * 1. NULL
 * 2. 值类型：原始类型，及其包装类型，{@link String}，{@link TypeId}
 * 3. 简单对象(POJO)
 * 4. 容器对象：MAP,COLLECTION,ARRAY。并没有基于{@link Iterable}做支持，而是基于的{@link Collection}
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/17
 * github - https://github.com/hl845740757
 */
public enum BinaryTag implements IndexableEnum {

    /**
     * NULL
     */
    NULL(0),

    // ----------------------------------------- 值类型 -----------------------------
    BYTE(1),
    SHORT(2),
    CHAR(3),
    INT(4),
    LONG(5),
    FLOAT(6),
    DOUBLE(7),
    BOOLEAN(8),
    STRING(9),
    // --------------------------------------- 简单对象 -------------------------------

    /**
     * 简单对象 - 非容器对象
     * 它必须存在唯一识别码，才能定位到对应的codec
     */
    POJO(10),

    // --------------------------------------- 容器对象 --------------------------------

    /**
     * 数组
     */
    ARRAY(11),

    /**
     * 集合支持
     * 如果一个字段/参数的声明类型是{@link Collection}，那么那么适用该类型。
     * 如果需要更细化的集合需求，请了解{@link com.wjybxx.fastjgame.net.binary.Impl}注解
     */
    COLLECTION(12),

    /**
     * Map支持
     * 如果一个字段/参数的声明类型是{@link Map}，那么适用该类型。
     * 如果需要更细化的map需求，请了解{@link com.wjybxx.fastjgame.net.binary.Impl}注解
     */
    MAP(13),

    // --------------------------------------- 标记 --------------------------------

    /**
     * 表示非值类型 - 只出现在数组编解码中
     */
    UNKNOWN(14);

    private final int number;

    BinaryTag(int number) {
        this.number = number;
    }

    private static final IndexableEnumMapper<BinaryTag> mapper = EnumUtils.mapping(values(), true);

    public static BinaryTag forNumber(int number) {
        return mapper.forNumber(number);
    }

    @Override
    public int getNumber() {
        return number;
    }
}
