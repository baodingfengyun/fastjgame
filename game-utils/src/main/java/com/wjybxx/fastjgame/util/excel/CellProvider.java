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

/**
 * @author wjybxx
 * date - 2020/12/1
 * github - https://github.com/hl845740757
 */
public abstract class CellProvider {

    public abstract ValueCell getCell(String name);

    @Nonnull
    public String readAsString(String name) {
        return getCell(name).readAsString();
    }

    public int readAsInt(String name) {
        return getCell(name).readAsInt();
    }

    public long readAsLong(String name) {
        return getCell(name).readAsLong();
    }

    public float readAsFloat(String name) {
        return getCell(name).readAsFloat();
    }

    public double readAsDouble(String name) {
        return getCell(name).readAsDouble();
    }

    public boolean readAsBool(String name) {
        return getCell(name).readAsBool();
    }

    public <T> T readAsArray(String name, @Nonnull Class<T> typeToken) {
        return getCell(name).readAsArray(typeToken);
    }

    public short readAsShort(String name) {
        return getCell(name).readAsShort();
    }

    public byte readAsByte(String name) {
        return getCell(name).readAsByte();
    }
}
