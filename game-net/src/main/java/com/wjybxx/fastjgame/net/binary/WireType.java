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


import com.wjybxx.fastjgame.utils.EnumUtils;
import com.wjybxx.fastjgame.utils.entity.NumericalEntity;
import com.wjybxx.fastjgame.utils.entity.NumericalEntityMapper;

import java.util.Collection;
import java.util.Map;

/**
 * 数据类型
 * <h3>类型</h3>
 * 1. 原始类型
 * 2. NULL
 * 3. 简单对象
 * 4. 容器对象（MAP,COLLECTION,ARRAY），并没有基于{@link Iterable}做支持，而是基于的{@link Collection}
 * <h3>展开</h3>
 * 由于原始类型和容器对象的种类是确定的，因此将它们展开
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/17
 * github - https://github.com/hl845740757
 */
public enum WireType implements NumericalEntity {

    /**
     * NULL
     */
    NULL(0),

    // ------------------------------------------- 基础类型 -----------------------------
    /**
     * rawByte
     */
    BYTE(1),
    /**
     * uInt32
     */
    CHAR(2),
    /**
     * varInt32
     */
    SHORT(3),
    /**
     * varInt32
     */
    INT(4),
    /**
     * varInt64
     */
    LONG(5),
    /**
     * fixed32
     */
    FLOAT(6),
    /**
     * fixed64
     */
    DOUBLE(7),
    /**
     * rawByte
     */
    BOOLEAN(8),

    // --------------------------------------- 简单对象 -------------------------------

    /**
     * 简单对象 - 非容器对象
     * 它必须存在唯一识别码，才能定位到对应的codec
     */
    POJO(9),

    // --------------------------------------- 容器对象 --------------------------------

    /**
     * 数组
     */
    ARRAY(10),

    /**
     * 集合支持
     * 如果一个字段/参数的声明类型是{@link Collection}，那么那么适用该类型。
     * 如果需要更细化的集合需求，请了解{@link com.wjybxx.fastjgame.db.annotation.Impl}注解
     */
    COLLECTION(11),

    /**
     * Map支持
     * 如果一个字段/参数的声明类型是{@link Map}，那么适用该类型。
     * 如果需要更细化的map需求，请了解{@link com.wjybxx.fastjgame.db.annotation.Impl}注解
     */
    MAP(12),

    // --------------------------------------- 特定标识 --------------------------------

    /**
     * 标识编解码时，仅仅代表一个表示，不关心具体类型
     */
    UNKNOWN(13);

    private final int number;

    WireType(int number) {
        this.number = number;
    }

    private static final NumericalEntityMapper<WireType> mapper = EnumUtils.mapping(values(), true);

    public static WireType forNumber(int number) {
        return mapper.forNumber(number);
    }

    @Override
    public int getNumber() {
        return number;
    }
}
