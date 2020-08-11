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

import com.wjybxx.fastjgame.net.serialization.HashTypeIdMappingStrategy;
import com.wjybxx.fastjgame.util.ClassScanner;
import com.wjybxx.fastjgame.util.MethodHandleUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/8/8
 */
public class CollectionScanner {

    private final Collection<Supplier<? extends Collection<?>>> collectionFactories = new ArrayList<>(256);
    private final Collection<Supplier<? extends Map<?, ?>>> mapFactories = new ArrayList<>(256);
    private final Collection<Class<?>> arrayTypes = new ArrayList<>(16);

    private final MethodHandles.Lookup lookup = MethodHandles.lookup();

    public static ScanResult scan() {
        try {
            return new CollectionScanner().scanImpl();
        } catch (Throwable e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    public static class ScanResult {

        final Collection<Supplier<? extends Collection<?>>> collectionFactories;
        final Collection<Supplier<? extends Map<?, ?>>> mapFactories;
        final Collection<Class<?>> arrayTypes;

        ScanResult(Collection<Supplier<? extends Collection<?>>> collectionFactories,
                   Collection<Supplier<? extends Map<?, ?>>> mapFactories, Collection<Class<?>> arrayTypes) {
            this.collectionFactories = collectionFactories;
            this.mapFactories = mapFactories;
            this.arrayTypes = arrayTypes;
        }
    }

    private ScanResult scanImpl() throws Throwable {
        addArrayTypes();
        addJdkCollections();
        addFastutilColletions();
        return new ScanResult(collectionFactories, mapFactories, arrayTypes);
    }

    /**
     * 添加常用的数组类型
     */
    private void addArrayTypes() {
        // 基本类型(不包含字节数组)
        arrayTypes.add(int[].class);
        arrayTypes.add(long[].class);
        arrayTypes.add(float[].class);
        arrayTypes.add(double[].class);
        arrayTypes.add(char[].class);
        arrayTypes.add(short[].class);
        arrayTypes.add(boolean[].class);

        // 包装类型
        arrayTypes.add(Integer[].class);
        arrayTypes.add(Long[].class);
        arrayTypes.add(Float[].class);
        arrayTypes.add(Double[].class);
        arrayTypes.add(Character[].class);
        arrayTypes.add(Short[].class);
        arrayTypes.add(Boolean[].class);
        arrayTypes.add(Byte[].class);

        // 字符串数组
        arrayTypes.add(String[].class);
    }

    /**
     * 添加JDK的集合
     * 注意：需要特殊构造的集合，不可以添加，如{@link EnumSet} {@link TreeSet}
     */
    private void addJdkCollections() {
        // 其实常用的不多
        collectionFactories.add(ArrayList::new);
        collectionFactories.add(LinkedList::new);
        collectionFactories.add(ArrayDeque::new);

        collectionFactories.add(HashSet::new);
        collectionFactories.add(LinkedHashSet::new);

        mapFactories.add(HashMap::new);
        mapFactories.add(LinkedHashMap::new);
        mapFactories.add(IdentityHashMap::new);
    }

    private void addFastutilColletions() throws Throwable {
        scanPackage("it.unimi.dsi.fastutil", CollectionScanner::fastutilNameFilter);
    }

    private static boolean fastutilNameFilter(String name) {
        if (name.contains("Byte") || name.contains("Char") || name.contains("Bool")) {
            // 这三种实在不怎么用
            return false;
        }

        if (StringUtils.contains(name, '2') && (name.contains("Double") || name.contains("Float"))) {
            // float 和 double的map不常用
            return false;
        }
        return true;
    }

    private void scanPackage(final String packageName, final Predicate<String> namePredicate) throws Throwable {
        final Predicate<Class<?>> predicate = clazz -> isCollectionOrMap(clazz) && hasPublicNoArgsConstructor(clazz);
        final Set<Class<?>> classSet = ClassScanner.findClasses(packageName,
                namePredicate,
                predicate);

        for (Class<?> clazz : classSet) {
            final MethodHandle constructor = lookup.findConstructor(clazz, MethodType.methodType(void.class));
            if (Collection.class.isAssignableFrom(clazz)) {
                collectionFactories.add(collectionSupplier(constructor));
            } else {
                mapFactories.add(mapSupplier(constructor));
            }
        }
    }

    private static boolean isCollectionOrMap(Class<?> clazz) {
        return Collection.class.isAssignableFrom(clazz)
                || Map.class.isAssignableFrom(clazz);
    }

    private static boolean hasPublicNoArgsConstructor(Class<?> clazz) {
        if (Modifier.isAbstract(clazz.getModifiers())) {
            return false;
        }
        return Arrays.stream(clazz.getDeclaredConstructors())
                .filter(constructor -> Modifier.isPublic(constructor.getModifiers()))
                .anyMatch(constructor -> constructor.getParameterCount() == 0);
    }

    @SuppressWarnings("unchecked")
    private Supplier<? extends Collection<?>> collectionSupplier(MethodHandle constructor) throws Throwable {
        return (Supplier<? extends Collection<?>>) MethodHandleUtils.noArgsConstructorToSupplier(lookup, constructor, Collection.class);
    }

    @SuppressWarnings("unchecked")
    private Supplier<? extends Map<?, ?>> mapSupplier(MethodHandle constructor) throws Throwable {
        return (Supplier<? extends Map<?, ?>>) MethodHandleUtils.noArgsConstructorToSupplier(lookup, constructor, Map.class);
    }

    public static void main(String[] args) throws Exception {
        final BinarySerializer binarySerializer = BinarySerializer.newInstance(new HashTypeIdMappingStrategy(), scan());

        long instanceNum = 0;
        final int loop = 1000;
        final long startTimeMs = System.currentTimeMillis();
        for (Supplier<? extends Collection<?>> supplier : binarySerializer.collectionFactoryMap.values()) {
            createInstance(supplier, loop);
            instanceNum += loop;
        }
        final long endTimeMs = System.currentTimeMillis();
        System.out.println("instanceNum: " + instanceNum + ", costTimeMs: " + (endTimeMs - startTimeMs));
    }

    private static void createInstance(Supplier<? extends Collection<?>> supplier, final int loop) {
        for (int index = 0; index < loop; index++) {
            supplier.get();
        }
    }
}
