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

package com.wjybxx.fastjgame.utils;

import com.google.protobuf.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * 反射工具类
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/4/27 10:30
 * github - https://github.com/hl845740757
 */
public class ReflectionUtils {

    private static final Logger logger = LoggerFactory.getLogger(ReflectionUtils.class);

    private ReflectionUtils() {
        // close
    }

    /**
     * 寻找protoBuf消息的parser对象
     * 优先尝试protoBuf 3.x版本
     * 其次尝试protoBuf 2.x版本
     *
     * @param clazz protoBuffer class
     * @return parser
     */
    @SuppressWarnings("unchecked")
    public static <T> Parser<T> findParser(Class<T> clazz) throws ReflectiveOperationException {
        Objects.requireNonNull(clazz);
        try {
            // protoBuf3获取parser的静态方法 parser();
            Method method = clazz.getDeclaredMethod("parser");
            if (null != method) {
                method.setAccessible(true);
                return (Parser<T>) method.invoke(null);
            }
        } catch (Exception ignore) {
            logger.info("not protoBuf 3.x");
        }
        try {
            // proto2 静态parser域,public的
            Field field = clazz.getDeclaredField("PARSER");
            field.setAccessible(true);
            return (Parser<T>) field.get(null);
        } catch (Exception ignore) {
            logger.info("not protoBuf 2.x");
        }
        throw new ReflectiveOperationException("invalid protocol buffer class " + clazz.getSimpleName());
    }
}
