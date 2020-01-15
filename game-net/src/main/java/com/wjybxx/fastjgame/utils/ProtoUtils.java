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

package com.wjybxx.fastjgame.utils;

import com.google.protobuf.Internal;
import com.google.protobuf.Message;
import com.google.protobuf.Parser;
import com.google.protobuf.ProtocolMessageEnum;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * protocol buffer工具类
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/12/24
 * github - https://github.com/hl845740757
 */
public class ProtoUtils {

    /**
     * 寻找protoBuf消息的parser对象
     * 优先尝试protoBuf 3.x版本
     * 其次尝试protoBuf 2.x版本
     *
     * @param clazz protoBuffer class
     * @return parser
     */
    public static Parser<?> findParser(@Nonnull Class<? extends Message> clazz) {
        Objects.requireNonNull(clazz);
        try {
            final Method method = clazz.getDeclaredMethod("getDefaultInstance");
            method.setAccessible(true);
            final Message instance = (Message) method.invoke(null);
            return instance.getParserForType();
        } catch (Exception e) {
            throw new IllegalArgumentException("bad class " + clazz.getName(), e);
        }
    }

    public static Internal.EnumLiteMap<?> findMapper(@Nonnull Class<? extends ProtocolMessageEnum> clazz) {
        try {
            final Method method = clazz.getDeclaredMethod("internalGetValueMap");
            method.setAccessible(true);
            return (Internal.EnumLiteMap<?>) method.invoke(null);
        } catch (Exception e) {
            throw new IllegalArgumentException("bad class " + clazz.getName(), e);
        }
    }
}
