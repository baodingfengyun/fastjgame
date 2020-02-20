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
 * {@link EntitySerializer}的扫描器，会扫描指定包下所有的serializer并加入集合。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/20
 */
public class EntitySerializerScanner {

    private static final Set<String> SCAN_PACKAGES = Set.of("com.wjybxx.fastjgame");

    /**
     * bean -> beanSerializer (自动生成的beanSerializer 或 手动实现的)
     * 缓存起来，避免大量查找。
     */
    private static final Map<Class<?>, Class<? extends EntitySerializer<?>>> classBeanSerializerMap;

    static {
        final Set<Class<?>> allSerializerClass = scan();

        classBeanSerializerMap = new IdentityHashMap<>(allSerializerClass.size());

        mapping(allSerializerClass);
    }

    private static Set<Class<?>> scan() {
        return SCAN_PACKAGES.stream()
                .map(scanPackage -> ClassScanner.findClasses(scanPackage,
                        name -> true,
                        EntitySerializerScanner::isEntitySerializer))
                .flatMap(Collection::stream)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static boolean isEntitySerializer(Class<?> c) {
        return !Modifier.isAbstract(c.getModifiers()) && EntitySerializer.class.isAssignableFrom(c);
    }

    @SuppressWarnings("unchecked")
    private static void mapping(Set<Class<?>> allSerializerClass) {
        for (Class<?> clazz : allSerializerClass) {
            final Class<? extends EntitySerializer<?>> serializerClass = (Class<? extends EntitySerializer<?>>) clazz;
            final Class<?> entityClass = TypeParameterFinder.findTypeParameterUnsafe(serializerClass, EntitySerializer.class, "T");

            if (entityClass == Object.class) {
                throw new UnsupportedOperationException("SerializerImpl must declare type parameter");
            }

            // 需要检测重复，如果出现两个serializer负责一个类的序列化，则用户应该解决
            if (classBeanSerializerMap.containsKey(entityClass)) {
                throw new UnsupportedOperationException(entityClass.getSimpleName() + " has more than one serializer");
            }

            classBeanSerializerMap.put(entityClass, serializerClass);
        }
    }

    /**
     * 判断一个类是否存在对应的{@link EntitySerializer}
     */
    public static <T> boolean hasSerializer(Class<T> messageClass) {
        return classBeanSerializerMap.containsKey(messageClass);
    }

    /**
     * 获取消息类对应的序列化辅助类
     *
     * @return 序列化辅助类
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public static <T> Class<? extends EntitySerializer<T>> getSerializerClass(Class<T> messageClass) {
        return (Class<? extends EntitySerializer<T>>) classBeanSerializerMap.get(messageClass);
    }

    /**
     * 返回所有的自定义实体类
     */
    public static Set<Class<?>> getAllCustomEntityClasses() {
        return Collections.unmodifiableSet(classBeanSerializerMap.keySet());
    }

}
