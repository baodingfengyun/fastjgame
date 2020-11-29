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

package com.wjybxx.fastjgame.reload.excel;

import com.wjybxx.fastjgame.util.constant.AbstractConstant;
import com.wjybxx.fastjgame.util.constant.ConstantPool;

/**
 * excel页签的名字。
 *
 * @param <T> 读表结果的类型
 * @author wjybxx
 * date - 2020/11/25
 * github - https://github.com/hl845740757
 */
public final class SheetName<T> extends AbstractConstant<SheetName<T>> {

    private SheetName(int id, String name) {
        super(id, name);
    }

    /**
     * 必须作为第一个字段，在其它字段使用它之前初始化。
     * 注意：该字段会被置为null，这并不是常用模式
     */
    private static final ConstantPool<SheetName<Object>> SHEET_NAME_POOL = new ConstantPool<>(SheetName::new);

    @SuppressWarnings("unchecked")
    public static <T> SheetName<T> valueOf(String name) {
        return (SheetName<T>) SHEET_NAME_POOL.valueOf(name);
    }

    @SuppressWarnings("unchecked")
    public static <T> SheetName<T> newInstance(String name) {
        return (SheetName<T>) SHEET_NAME_POOL.newInstance(name);
    }

    @SuppressWarnings("unchecked")
    public static <T> SheetName<T> forNameThrowable(String name) {
        return (SheetName<T>) SHEET_NAME_POOL.getOrThrow(name);
    }

}