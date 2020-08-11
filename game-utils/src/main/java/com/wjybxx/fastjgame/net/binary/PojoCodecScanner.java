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

import com.wjybxx.fastjgame.util.ClassScanner;
import com.wjybxx.fastjgame.util.reflect.TypeParameterFinder;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * {@link PojoCodecImpl}的扫描器，会扫描指定包下所有的serializer并加入集合。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/20
 */
public class PojoCodecScanner {

    private static final Set<String> SCAN_PACKAGES = Set.of("com.wjybxx.fastjgame");

    public static Map<Class<?>, Class<? extends PojoCodecImpl<?>>> scan() {
        return scan(SCAN_PACKAGES);
    }

    public static Map<Class<?>, Class<? extends PojoCodecImpl<?>>> scan(Set<String> packages) {
        return packages.stream()
                .map(scanPackage -> ClassScanner.findClasses(scanPackage, name -> true, PojoCodecScanner::isPojoCodecImpl))
                .flatMap(Collection::stream)
                .collect(Collectors.toMap(PojoCodecScanner::findEncoderClass, PojoCodecScanner::castCodecClass));
    }

    private static boolean isPojoCodecImpl(Class<?> clazz) {
        if (Modifier.isAbstract(clazz.getModifiers())) {
            return false;
        }
        if (!PojoCodecImpl.class.isAssignableFrom(clazz)) {
            return false;
        }
        return hasNoArgsConstructor(clazz);
    }

    private static boolean hasNoArgsConstructor(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredConstructors())
                .anyMatch(constructor -> constructor.getParameterCount() == 0);
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends PojoCodecImpl<?>> castCodecClass(Class<?> clazz) {
        return (Class<? extends PojoCodecImpl<?>>) clazz;
    }

    private static Class<?> findEncoderClass(Class<?> clazz) {
        final Class<? extends PojoCodecImpl<?>> codecClass = castCodecClass(clazz);
        return TypeParameterFinder.findTypeParameterUnsafe(codecClass, PojoCodecImpl.class, "T");
    }

}
