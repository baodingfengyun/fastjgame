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

import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * 默认的单元格解析器，为避免引入不必要的依赖，识别为json时只是当作普通字符串，不解析json。
 *
 * @author wjybxx
 * date - 2020/11/21
 * github - https://github.com/hl845740757
 */
@Immutable
public class DefaultCellValueParser implements CellValueParser {

    // 基本数据类型
    private static final String STRING = "string";
    private static final String INT32 = "int32";
    private static final String INT64 = "int64";
    private static final String FLOAT = "float";
    private static final String DOUBLE = "double";
    private static final String BOOL = "bool";
    /**
     * JSON用于配置复杂的数据结构
     */
    private static final String JSON = "json";

    private static final Set<String> BASIC_TYPES = Set.of(STRING, INT32, INT64, FLOAT, DOUBLE, BOOL);
    private static final Set<String> SUPPORTED_TYPES;

    private static final List<String> INTEGER_TYPES = List.of(INT32, INT64);
    private static final List<String> FLOAT_DOUBLE_TYPES = List.of(FLOAT, DOUBLE);
    private static final List<String> BOOL_TYPE = List.of(BOOL);

    static {
        Set<String> tempSupportedTypes = Sets.newHashSetWithExpectedSize(BASIC_TYPES.size() * 3 + 1);
        tempSupportedTypes.addAll(BASIC_TYPES);
        // 一维和二维数组
        for (String typeString : BASIC_TYPES) {
            tempSupportedTypes.add(typeString + "[]");
            tempSupportedTypes.add(typeString + "[][]");
        }
        // json
        tempSupportedTypes.add(JSON);
        SUPPORTED_TYPES = Set.copyOf(tempSupportedTypes);
    }

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
        return null == value ? "" : value;
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

    @Override
    public <T> T readAsArray(@Nonnull String typeString, @Nullable String value, @Nonnull Class<T> typeToken) {
        Objects.requireNonNull(typeToken, "typeToken");
        Objects.requireNonNull(value, "value");
        checkArrayTypeString(typeString);
        checkArrayTypeToken(typeToken);
        checkArrayDimensional(typeString, typeToken);

        // 暂时的解决方案有点笨拙
        final int arrayDimensional = getArrayDimensional(typeToken);
        if (arrayDimensional == 1) {
            @SuppressWarnings("unchecked") final T result = (T) parseOneDimensionalArray(typeString, typeToken, value);
            return result;
        } else {
            @SuppressWarnings("unchecked") final T result = (T) parseTwoDimensionalArray(typeString, typeToken, value);
            return result;
        }
    }

    /**
     * 解析一维数组
     */
    private static Object parseOneDimensionalArray(String typeString, final Class<?> typeToken, final String value) {
        final String componentTypeString = typeString.substring(0, typeString.indexOf('['));
        final Stream<String> stringStream = Arrays.stream(StringUtils.split(deleteLeftAndRightBraces(value), ','))
                .map(String::trim);

        final Class<?> componentType = typeToken.getComponentType();
        if (componentType == String.class) {
            return stringStream.toArray(String[]::new);
        }

        if (componentType == int.class) {
            checkTypeString(INTEGER_TYPES, componentTypeString);
            return stringStream
                    .mapToInt(Integer::parseInt)
                    .toArray();
        }

        if (componentType == long.class) {
            checkTypeString(INTEGER_TYPES, componentTypeString);
            return stringStream
                    .mapToLong(Long::parseLong)
                    .toArray();
        }

        if (componentType == double.class) {
            checkTypeString(FLOAT_DOUBLE_TYPES, componentTypeString);
            return stringStream
                    .mapToDouble(Double::parseDouble)
                    .toArray();
        }

        final String[] elements = stringStream.toArray(String[]::new);
        if (componentType == float.class) {
            checkTypeString(FLOAT_DOUBLE_TYPES, componentTypeString);
            float[] result = new float[elements.length];
            for (int index = 0; index < elements.length; index++) {
                result[index] = Float.parseFloat(elements[index]);
            }
            return result;
        }

        if (componentType == boolean.class) {
            checkTypeString(BOOL_TYPE, componentTypeString);
            boolean[] result = new boolean[elements.length];
            for (int index = 0; index < elements.length; index++) {
                result[index] = parseBool(elements[index]);
            }
            return result;
        }

        if (componentType == short.class) {
            checkTypeString(INTEGER_TYPES, componentTypeString);
            short[] result = new short[elements.length];
            for (int index = 0; index < elements.length; index++) {
                result[index] = Short.parseShort(elements[index]);
            }
            return result;
        }

        if (componentType == byte.class) {
            checkTypeString(INTEGER_TYPES, componentTypeString);
            byte[] result = new byte[elements.length];
            for (int index = 0; index < elements.length; index++) {
                result[index] = Byte.parseByte(elements[index]);
            }
            return result;
        }
        throw new IllegalArgumentException("unsupported component type: " + componentType);
    }

    /**
     * 删除左右大括号
     *
     * @param value 数组字符串
     * @return 数组的内容部分
     */
    private static String deleteLeftAndRightBraces(String value) {
        final int startIndex = value.indexOf('{');
        final int endIndex = value.lastIndexOf('}');
        if (startIndex < 0 || endIndex < 0 || startIndex > endIndex) {
            throw new IllegalArgumentException("brace mismatch, value: " + value);
        }
        if (startIndex + 1 == endIndex) {
            return "";
        }
        return value.substring(startIndex + 1, endIndex);
    }

    /**
     * 解析二维数组
     */
    private Object parseTwoDimensionalArray(String typeString, final Class<?> typeToken, final String value) {
        final String elementsString = deleteLeftAndRightBraces(value);
        final IntList leftBracesIndexes = statisticCharIndexes(elementsString, '{');
        final IntList rightBracesIndexes = statisticCharIndexes(elementsString, '}');
        if (leftBracesIndexes.size() != rightBracesIndexes.size()) {
            throw new IllegalArgumentException("brace mismatch, value: " + value);
        }

        final Object result = Array.newInstance(typeToken.getComponentType(), leftBracesIndexes.size());
        for (int index = 0; index < leftBracesIndexes.size(); index++) {
            final String oneDimensionalArrayString = elementsString.substring(leftBracesIndexes.getInt(index), rightBracesIndexes.getInt(index) + 1);
            final Object oneDimensionalArray = parseOneDimensionalArray(typeString, typeToken.getComponentType(), oneDimensionalArrayString);
            Array.set(result, index, oneDimensionalArray);
        }
        return result;
    }

    private static IntList statisticCharIndexes(String value, int ch) {
        final IntList result = new IntArrayList(8);
        int startIndex = 0;
        int index;
        while ((index = value.indexOf(ch, startIndex)) >= 0) {
            result.add(index);
            startIndex = index + 1;
        }
        return result;
    }

    private static void checkArrayTypeString(String typeString) {
        if (!typeString.endsWith("[]")) {
            throw new CellTypeIncompatibleException(List.of("[]", "[][]"), typeString);
        }
    }

    private static void checkArrayTypeToken(Class<?> typeToken) {
        if (!typeToken.isArray()) {
            throw new IllegalArgumentException("typeToken must be an array, typeToken: " + typeToken);
        }
    }

    private static void checkArrayDimensional(String typeString, Class<?> typeToken) {
        final int typeTokenDimensional = getArrayDimensional(typeToken);
        final int typeStringDimensional = getArrayDimensional(typeString);
        if (typeTokenDimensional != typeStringDimensional) {
            final String msg = String.format("typeString and typeToken have different dimensions, typeString: %s, typeToken: %s",
                    typeString, typeToken.toString());
            throw new IllegalArgumentException(msg);
        }
    }

    private static int getArrayDimensional(Class<?> typeToken) {
        int result = 0;
        while (typeToken.isArray()) {
            result++;
            typeToken = typeToken.getComponentType();
        }
        return result;
    }

    private static int getArrayDimensional(String typeString) {
        if (typeString.endsWith("[][]")) {
            return 2;
        } else {
            assert typeString.endsWith("[]") : typeString;
            return 1;
        }
    }
}
