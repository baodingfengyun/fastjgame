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
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 默认的单元格解析器，为避免引入不必要的依赖，识别为json时只是当作普通字符串，不解析json。
 *
 * @author wjybxx
 * date - 2020/11/21
 * github - https://github.com/hl845740757
 */
public class DefaultCellValueParser implements CellValueParser {

    private static final String STRING = "string";
    private static final String INT32 = "int32";
    private static final String INT64 = "int64";
    private static final String FLOAT = "float";
    private static final String DOUBLE = "double";
    private static final String BOOL = "bool";

    private static final Set<String> SUPPORTED_TYPES = Set.of(STRING, INT32, INT64, FLOAT, DOUBLE, BOOL);

    private static final List<String> STRING_TYPE = List.of(STRING);
    private static final List<String> INTEGER_TYPES = List.of(INT32, INT64);
    private static final List<String> FLOAT_DOUBLE_TYPES = List.of(FLOAT, DOUBLE);
    private static final List<String> BOOL_TYPE = List.of(BOOL);

    private static boolean isExpectedType(List<String> expectedTypeStrings, String typeString) {
        for (int index = 0, size = expectedTypeStrings.size(); index < size; index++) {
            final String expectedTypeString = expectedTypeStrings.get(index);
            if (expectedTypeString.equals(typeString)) {
                return true;
            }
        }
        return false;
    }

    private static void checkTypeString(List<String> expectedTypeStrings, String typeString) {
        if (!isExpectedType(expectedTypeStrings, typeString)) {
            throw new CellTypeIncompatibleException(expectedTypeStrings, typeString);
        }
    }

    @Override
    public Set<String> supportedTypes() {
        return SUPPORTED_TYPES;
    }

    @Override
    public String readAsString(@Nonnull String typeString, @Nullable String value) {
        return Objects.requireNonNullElse(value, "");
    }

    @Override
    public int readAsInt(@Nonnull String typeString, @Nullable String value) {
        Objects.requireNonNull(value, "value");
        checkTypeString(INTEGER_TYPES, typeString);
        return Integer.parseInt(value);
    }

    @Override
    public long readAsLong(@Nonnull String typeString, @Nullable String value) {
        Objects.requireNonNull(value, "value");
        checkTypeString(INTEGER_TYPES, typeString);
        return Long.parseLong(value);
    }

    @Override
    public float readAsFloat(@Nonnull String typeString, @Nullable String value) {
        Objects.requireNonNull(value, "value");
        checkTypeString(FLOAT_DOUBLE_TYPES, typeString);
        return Float.parseFloat(value);
    }

    @Override
    public double readAsDouble(@Nonnull String typeString, @Nullable String value) {
        Objects.requireNonNull(value, "value");
        checkTypeString(FLOAT_DOUBLE_TYPES, typeString);
        return Double.parseDouble(value);
    }

    @Override
    public boolean readAsBool(@Nonnull String typeString, @Nullable String value) {
        Objects.requireNonNull(value, "value");
        checkTypeString(BOOL_TYPE, typeString);
        return parseBool(value);
    }

    private static boolean parseBool(@Nonnull String value) {
        return value.equals("1") ||
                value.equalsIgnoreCase("TRUE") ||
                value.equalsIgnoreCase("YES") ||
                value.equalsIgnoreCase("Y");
    }

    @Override
    public <T> T readAsArray(@Nonnull String typeString, @Nullable String value, @Nonnull Class<T> typeToken) {
        // TODO
        return null;
    }

    @Override
    public short readAsShort(@Nonnull String typeString, @Nullable String value) {
        Objects.requireNonNull(value, "value");
        checkTypeString(INTEGER_TYPES, typeString);
        return Short.parseShort(value);
    }

    @Override
    public byte readAsByte(@Nonnull String typeString, @Nullable String value) {
        Objects.requireNonNull(value, "value");
        checkTypeString(INTEGER_TYPES, typeString);
        return Byte.parseByte(value);
    }

}
