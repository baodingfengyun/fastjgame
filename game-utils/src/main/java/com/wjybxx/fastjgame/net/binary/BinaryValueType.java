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

    /**
     * protoBuf的{@link com.google.protobuf.MessageLite}
     * 序列化格式：tag + length + typeId + content
     * 如果将其看作容器的话，它是有单个字节数组属性的容器。它的内容需要按照{@link #BINARY}的格式写入，这会多5个字节的开销。
     * 如果使用{@link ObjectWriter}中定义的方法写入，则需要先序列化为字节数组，这回影响编解码效率。
     * 考虑到比重较大，我们将其列为基本值类型，以方便底层对其优化，直接写入流中。
     * 至于protoBuf的枚举，使用比例和序列化成本并不高，当作普通的单值容器序列化即可。
     * <p>
     * Q: 为什么length要方前面？
     * A: length放前面是有好处的，可以方便快速拆包，解包，因为有效内容是连续的。
     */
    MESSAGE(11),

    // --------------------------------------- 容器对象 -------------------------------
    /**
     * 对象（容器） - 自定义Bean,Map,Collection,Array等容器都属于该类型。
     * 序列化格式： tag + length + typeId + value,value,value....
     * 注意：
     * 1. 容器的每一个值都是调用{@link ObjectWriter}中的方法进行序列化的，否则就不算容器。
     * 2. 你可以将对方序列化的一个对象，读取为其它容器类型。比如对方序列化的任意容器都可以读取为一个ArrayList。
     */
    OBJECT(12);

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
