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
 * {@link ValueCell}的默认实现，将解析过程转移给{@link CellValueParser}，以实现各项目的扩展。
 *
 * @author wjybxx
 * date - 2020/11/21
 * github - https://github.com/hl845740757
 */
public class DefaultValueCell implements ValueCell {

    private final CellValueParser parser;
    private final String name;
    private final String type;
    private final String value;

    public DefaultValueCell(CellValueParser parser, String name, String type, String value) {
        this.parser = parser;
        this.name = name;
        this.type = type;
        this.value = value;
    }

    @Nonnull
    @Override
    public String name() {
        return name;
    }

    @Nonnull
    @Override
    public String type() {
        return type;
    }

    @Nullable
    @Override
    public String value() {
        return value;
    }

    @Nonnull
    @Override
    public final String readAsString() {
        return parser.readAsString(type, value);
    }

    @Override
    public final int readAsInt() {
        return parser.readAsInt(type, value);
    }

    @Override
    public final long readAsLong() {
        return parser.readAsLong(type, value);
    }

    @Override
    public final float readAsFloat() {
        return parser.readAsFloat(type, value);
    }

    @Override
    public final double readAsDouble() {
        return parser.readAsDouble(type, value);
    }

    @Override
    public final boolean readAsBool() {
        return parser.readAsBool(type, value);
    }

    @Override
    public final <T> T readAsArray(@Nonnull Class<T> typeToken) {
        return parser.readAsArray(type, value, typeToken);
    }

    @Override
    public final short readAsShort() {
        return parser.readAsShort(type, value);
    }

    @Override
    public final byte readAsByte() {
        return parser.readAsByte(type, value);
    }

    @Override
    public String toString() {
        return "DefaultValueCell{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
