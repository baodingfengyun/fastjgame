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
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/**
 * {@link EntitySerializer}的扫描器，会扫描包下所有的serializer并加入集合。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/20
 */
public class EntitySerializerScanner {

    /**
     * bean -> beanSerializer (自动生成的beanSerializer 或 手动实现的)
     * 缓存起来，避免大量查找。
     */
    private static final Map<Class<?>, Class<? extends EntitySerializer<?>>> classBeanSerializerMap;

    static {
        final Set<Class<?>> allSerializerClass = ClassScanner.findClasses("com.wjybxx.fastjgame", name -> true, EntitySerializerScanner::isBeanSerializer);
        classBeanSerializerMap = new IdentityHashMap<>(allSerializerClass.size());

        for (Class<?> clazz : allSerializerClass) {
            @SuppressWarnings("unchecked") final Class<? extends EntitySerializer<?>> serializerClass = (Class<? extends EntitySerializer<?>>) clazz;

            final Class<?> entityClass = TypeParameterFinder.findTypeParameterUnsafe(serializerClass, EntitySerializer.class, "T");
            if (entityClass == Object.class) {
                throw new UnsupportedOperationException("SerializerImpl must declare type parameter");
            }

            if (classBeanSerializerMap.containsKey(entityClass)) {
                throw new UnsupportedOperationException(entityClass.getSimpleName() + " has more than one serializer");
            }

            classBeanSerializerMap.put(entityClass, serializerClass);
        }
    }

    private static boolean isBeanSerializer(Class<?> c) {
        return !Modifier.isAbstract(c.getModifiers()) && EntitySerializer.class.isAssignableFrom(c);
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
    @Nullable
    @SuppressWarnings("unchecked")
    public static <T> Class<EntitySerializer<T>> getSerializerClass(Class<T> messageClass) {
        return (Class<EntitySerializer<T>>) classBeanSerializerMap.get(messageClass);
    }
}
