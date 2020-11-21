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

package com.wjybxx.fastjgame.util.excel;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 注意：
 * 1. 一定不要简单地使用 强制类型转换 来返回结果，比如double转其它数值类型，这可以导致错误的配置被忽略！
 * 2. 如果单元格的类型不匹配，则抛出{@link CellTypeIncompatibleException}异常。
 * 3. 当单元格内容无法转换为数字时，则抛出{@link NumberFormatException}异常。
 *
 * @author wjybxx
 * date - 2020/11/20
 * github - https://github.com/hl845740757
 */
public interface CellValue {

    /**
     * @return 返回该单元格的原始字符串
     * @apiNote 无论该单元格的是什么类型，都应该能返回对应的字符串类型。
     * 为避免引入不必要的依赖，对于json类型，请自行解析返回的字符串。
     */
    @Nonnull
    String readAsString();

    /**
     * @return 如果单元格是约定的int类型，则返回对应的int值
     */
    int readAsInt();

    /**
     * @return 如果单元格是约定的int或long类型，则返回对应的long值
     */
    long readAsLong();

    /**
     * @return 如果单元格是约定的float类型，则返回对应的float值
     */
    float readAsFloat();

    /**
     * @return 如果单元格是约定的float或double类型，则返回对应的double值
     */
    double readAsDouble();

    /**
     * @return 如果单元格是约定的bool类型，则返回对应的bool值。
     */
    boolean readAsBool();

    /**
     * @param typeToken 类型令牌，用于捕获类型信息
     * @return 如果单元格是约定的数组类型，则返回对应的数组类型。
     */
    <T> T readAsArray(@Nonnull Class<T> typeToken);

    /**
     * @return @return 如果单元格是约定的short类型，则返回对应的short值
     */
    short readAsShort();

    /**
     * @return @return 如果单元格是约定的byte类型，则返回对应的byte值
     */
    byte readAsByte();

    // ------------------------------------------------------------- 应用逻辑一般不使用 --------------------------------

    /**
     * @return 获取单元格的对应的命名，不为null。
     */
    @Nonnull
    String name();

    /**
     * @return 获取单元格的类型字符串，不为null。
     */
    @Nonnull
    String type();

    /**
     * @return 获取单元格原始的字符串，可能为null。
     */
    @Nullable
    String value();
}
