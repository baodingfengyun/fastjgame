/*
 * Copyright 2019 wjybxx
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wjybxx.fastjgame.db.core;

import javax.annotation.Nonnull;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/4/21
 */
public class DefaultTypeIdentifier implements TypeIdentifier {

    private final Class<?> type;
    private final String name;
    private final long number;

    public DefaultTypeIdentifier(Class<?> type, String name, long number) {
        this.type = type;
        this.name = name;
        this.number = number;
    }

    @Nonnull
    @Override
    public Class<?> type() {
        return type;
    }

    @Nonnull
    @Override
    public String uniqueName() {
        return name;
    }

    @Override
    public long uniqueNumber() {
        return number;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final DefaultTypeIdentifier that = (DefaultTypeIdentifier) o;
        return type == that.type
                && number == that.number
                && name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return 31 * type.hashCode()
                + 31 * name.hashCode()
                + Long.hashCode(number);
    }
}
