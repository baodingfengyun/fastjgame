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


import com.google.protobuf.AbstractMessage;
import com.google.protobuf.ProtocolMessageEnum;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Map;

/**
 * 数据类型
 * 不建议大量使用数组类型，建议使用集合，这里并不对数组做完全的支持。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/17
 * github - https://github.com/hl845740757
 */
public class WireType {

    /**
     * NULL
     */
    static final byte NULL = 0;

    // ------------------------------------------- 基础类型 -----------------------------
    /**
     * rawByte
     */
    static final byte BYTE = 1;
    /**
     * uInt
     */
    static final byte CHAR = 2;
    /**
     * varInt
     */
    static final byte SHORT = 3;
    /**
     * varInt(不要修改名字) - INT开放给了许多地方
     */
    static final byte INT = 4;
    /**
     * varInt64
     */
    static final byte LONG = 5;
    /**
     * fixed32
     */
    static final byte FLOAT = 6;
    /**
     * fixed64
     */
    static final byte DOUBLE = 7;
    /**
     * rawByte
     */
    static final byte BOOLEAN = 8;

    /**
     * 字符串 LENGTH_DELIMITED
     */
    static final byte STRING = 9;

    // ---------------------------------------- 容器 ----------------------------------

    /**
     * 数组
     */
    static final byte ARRAY = 10;

    /**
     * 集合支持
     * 如果一个字段/参数的声明类型是{@link Collection}，那么那么适用该类型。
     * 如果需要更细化的集合需求，请了解{@link com.wjybxx.fastjgame.db.annotation.Impl}注解
     */
    static final byte COLLECTION = 11;
    /**
     * Map支持
     * 如果一个字段/参数的声明类型是{@link Map}，那么适用该类型。
     * 如果需要更细化的map需求，请了解{@link com.wjybxx.fastjgame.db.annotation.Impl}注解
     */
    static final byte MAP = 12;

    // -------------------------------------- 应用扩展类型 ----------------------------------

    /**
     * protobuf的Message LENGTH_DELIMITED
     */
    static final byte PROTO_MESSAGE = 13;
    /**
     * protoBuf的枚举
     */
    static final byte PROTO_ENUM = 14;

    /**
     * 存在对应的{@link EntitySerializer}的类型。
     * 可能是通过注解处理器生成的，也可能是手动实现的。
     */
    static final byte CUSTOM_ENTITY = 15;

    // ------------------------------------ 运行时才知道的 -----------------------------
    /**
     * 动态类型 - 运行时才能确定的类型（它是标记类型）
     */
    static final byte RUN_TIME = 16;

}
