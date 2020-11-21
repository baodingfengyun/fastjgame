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
import java.util.Objects;

/**
 * 表头单元格。
 * 对于表头单元格，不应该尝试读取内容。
 *
 * @author wjybxx
 * date - 2020/11/21
 * github - https://github.com/hl845740757
 */
class HeaderCellValue implements CellValue {

    private final String name;
    private final String type;
    private final String value;

    public HeaderCellValue(String name, String type, String value) {
        this.name = Objects.requireNonNull(name, "name");
        this.type = Objects.requireNonNull(type, "type");
        this.value = Objects.requireNonNull(value, "value");
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

    // 以下方法不应该被访问

    @Nonnull
    @Override
    public String readAsString() {
        throw new AssertionError();
    }

    @Override
    public int readAsInt() {
        throw new AssertionError();
    }

    @Override
    public long readAsLong() {
        throw new AssertionError();
    }

    @Override
    public float readAsFloat() {
        throw new AssertionError();
    }

    @Override
    public double readAsDouble() {
        throw new AssertionError();
    }

    @Override
    public boolean readAsBool() {
        throw new AssertionError();
    }

    @Override
    public <T> T readAsArray(@Nonnull Class<T> typeToken) {
        throw new AssertionError();
    }

    @Override
    public short readAsShort() {
        throw new AssertionError();
    }

    @Override
    public byte readAsByte() {
        throw new AssertionError();
    }

}
