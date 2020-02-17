/*
 *
 *  *  Copyright 2019 wjybxx
 *  *
 *  *  Licensed under the Apache License, Version 2.0 (the "License");
 *  *  you may not use this file except in compliance with the License.
 *  *  You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *  Unless required by applicable law or agreed to iBn writing, software
 *  *  distributed under the License is distributed on an "AS IS" BASIS,
 *  *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *  See the License for the specific language governing permissions and
 *  *  limitations under the License.
 *
 */

package com.wjybxx.fastjgame.apt.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/18
 * github - https://github.com/hl845740757
 */
public class AptReflectUtils {

    /**
     * 获取对的无参构造方法
     * 生成的代码调用
     */
    public static <T> Constructor<T> getNoArgsConstructor(Class<T> clazz) {
        try {
            final Constructor<T> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor;
        } catch (NoSuchMethodException e) {
            return rethrow(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <R, T extends Throwable> R rethrow(final Throwable throwable) throws T {
        throw (T) throwable;
    }

    /**
     * 生成的代码调用
     */
    public static Field getDeclaredField(Class<?> clazz, String fieldName) {
        try {
            final Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field;
        } catch (Throwable e) {
            return rethrow(e);
        }
    }
}
