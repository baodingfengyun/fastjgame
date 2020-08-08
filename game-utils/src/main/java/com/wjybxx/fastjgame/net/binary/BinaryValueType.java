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


import com.wjybxx.fastjgame.net.serialization.TypeId;
import com.wjybxx.fastjgame.util.EnumUtils;
import com.wjybxx.fastjgame.util.dsl.IndexableEnum;
import com.wjybxx.fastjgame.util.dsl.IndexableEnumMapper;

/**
 * 值类型
 * 1. 普通值：原始类型，及其包装类型，{@link String}，字节数组，NULL
 * 2. 容器值：{@link #OBJECT}
 * <p>
 * Q: 如何解决常用数组和集合的解析？
 * A: 为其分配{@link TypeId}。简单稳定的方式：扫描指定包。可以使用{@link CollectionScanner}
 * <p>
 * Q: 为什么只有{@link #OBJECT}类型的容器？
 * A: 分析之后，在不写fieldNumber(ProtoBuf)或fieldName(Bson)之后，一般对象和数组以及各种集合的结构就是相同的了，就只是值的集合。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/17
 * github - https://github.com/hl845740757
 */
public enum BinaryValueType implements IndexableEnum {

    // ----------------------------------------- 基本值 -----------------------------
    NULL(0),

    BYTE(1),
    SHORT(2),
    CHAR(3),
    INT(4),
    LONG(5),
    FLOAT(6),
    DOUBLE(7),
    BOOLEAN(8),
    /**
     * 字符串
     */
    STRING(9),
    /**
     * 字节数组
     */
    BINARY(10),

    // --------------------------------------- 容器对象 -------------------------------
    /**
     * 对象 - 自定义Bean,Map,Collection,Array等容器都属于该类型。
     * 序列化格式： type + typeId + value,value,value....
     * 注意：你可以将对方序列化的一个对象，读取为其它容器类型。
     */
    OBJECT(11);

    private final int number;

    BinaryValueType(int number) {
        this.number = number;
    }

    private static final IndexableEnumMapper<BinaryValueType> mapper = EnumUtils.mapping(values(), true);

    public static BinaryValueType forNumber(int number) {
        return mapper.forNumber(number);
    }

    @Override
    public int getNumber() {
        return number;
    }

}
