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

package com.wjybxx.fastjgame.net.binary;

import com.wjybxx.fastjgame.utils.ClassScanner;
import com.wjybxx.fastjgame.utils.reflect.TypeParameterFinder;

import javax.annotation.Nullable;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

/**
 * {@link PojoCodecImpl}的扫描器，会扫描指定包下所有的serializer并加入集合。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/20
 */
public class CodecScanner {

    private static final Set<String> SCAN_PACKAGES = Set.of("com.wjybxx.fastjgame");

    /**
     * bean -> beanSerializer (自动生成的beanSerializer 或 手动实现的)
     * 缓存起来，避免大量查找。
     */
    private static final Map<Class<?>, Class<? extends PojoCodecImpl<?>>> codecMap;

    static {
        final Set<Class<?>> allCodecClass = scan();

        codecMap = new IdentityHashMap<>(allCodecClass.size());

        mapping(allCodecClass);
    }

    private static Set<Class<?>> scan() {
        return SCAN_PACKAGES.stream()
                .map(scanPackage -> ClassScanner.findClasses(scanPackage, name -> true, CodecScanner::isPojoCodecImpl))
                .flatMap(Collection::stream)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static boolean isPojoCodecImpl(Class<?> c) {
        if (Modifier.isAbstract(c.getModifiers())) {
            return false;
        }
        if (!PojoCodecImpl.class.isAssignableFrom(c)) {
            return false;
        }
        if (c == ProtoMessageCodec.class || c == ProtoEnumCodec.class || c == CustomPojoCodec.class) {
            return false;
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private static void mapping(Set<Class<?>> allCodecClass) {
        for (Class<?> clazz : allCodecClass) {
            final Class<? extends PojoCodecImpl<?>> codecClass = (Class<? extends PojoCodecImpl<?>>) clazz;
            final Class<?> entityClass = TypeParameterFinder.findTypeParameterUnsafe(codecClass, PojoCodecImpl.class, "T");

            if (entityClass == Object.class) {
                throw new UnsupportedOperationException("SerializerImpl must declare type parameter");
            }

            // 需要检测重复，如果出现两个serializer负责一个类的序列化，则用户应该解决
            if (codecMap.containsKey(entityClass)) {
                throw new UnsupportedOperationException(entityClass.getSimpleName() + " has more than one serializer");
            }

            codecMap.put(entityClass, codecClass);
        }
    }

    /**
     * 判断一个类是否存在对应的{@link PojoCodecImpl}
     */
    public static <T> boolean hasCodec(Class<T> messageClass) {
        return codecMap.containsKey(messageClass);
    }

    /**
     * 获取消息类对应的序列化辅助类
     *
     * @return 序列化辅助类
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public static <T> Class<? extends PojoCodecImpl<T>> getCodecClass(Class<T> messageClass) {
        return (Class<? extends PojoCodecImpl<T>>) codecMap.get(messageClass);
    }

    /**
     * 返回所有的自定义实体类
     */
    public static Set<Class<?>> getAllCustomCodecClass() {
        return Collections.unmodifiableSet(codecMap.keySet());
    }

}
