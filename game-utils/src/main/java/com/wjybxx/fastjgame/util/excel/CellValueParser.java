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
import java.util.Set;

/**
 * 单元格内容解析器，用于自定义类型和解析。
 *
 * @author wjybxx
 * date - 2020/11/21
 * github - https://github.com/hl845740757
 */
public interface CellValueParser {

    /**
     * @return 返回支持的元素类型
     */
    Set<String> supportedTypes();

    /**
     * @return 获取单元格的字符串内容。
     */
    String readAsString(@Nonnull String typeString, @Nullable String value);

    /**
     * @return 如果单元格是约定的int类型，则返回对应的int值
     */
    int readAsInt(@Nonnull String typeString, @Nullable String value);

    /**
     * @return 如果单元格是约定的int或long类型，则返回对应的long值
     */
    long readAsLong(@Nonnull String typeString, @Nullable String value);

    /**
     * @return 如果单元格是约定的float类型，则返回对应的float值
     */
    float readAsFloat(@Nonnull String typeString, @Nullable String value);

    /**
     * @return 如果单元格是约定的float或double类型，则返回对应的double值
     */
    double readAsDouble(@Nonnull String typeString, @Nullable String value);

    /**
     * @return 如果单元格是约定的bool类型，则返回对应的bool值。
     */
    boolean readAsBool(@Nonnull String typeString, @Nullable String value);

    /**
     * @param typeToken 类型令牌，用于捕获类型信息
     * @return 如果单元格是约定的数组类型，则返回对应的数组类型。
     */
    <T> T readAsArray(@Nonnull String typeString, @Nullable String value, @Nonnull Class<T> typeToken);

    /**
     * @return @return 如果单元格是约定的short类型，则返回对应的short值
     */
    short readAsShort(@Nonnull String typeString, @Nullable String value);

    /**
     * @return @return 如果单元格是约定的byte类型，则返回对应的byte值
     */
    byte readAsByte(@Nonnull String typeString, @Nullable String value);

}
