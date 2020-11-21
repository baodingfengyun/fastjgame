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
 * 当excel单元格格式不兼容时抛出该异常
 *
 * @author wjybxx
 * date - 2020/11/21
 * github - https://github.com/hl845740757
 */
public class CellTypeIncompatibleException extends RuntimeException {

    private final String expectedType;
    private final String realType;

    public CellTypeIncompatibleException(@Nonnull String expectedType, @Nonnull String realType) {
        super(String.format("expectedType: %s, realType: %s", expectedType, realType));
        this.expectedType = expectedType;
        this.realType = realType;
    }

    public String getExpectedType() {
        return expectedType;
    }

    public String getRealType() {
        return realType;
    }
}
