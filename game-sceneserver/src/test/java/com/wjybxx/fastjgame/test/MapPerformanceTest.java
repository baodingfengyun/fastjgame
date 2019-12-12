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

package com.wjybxx.fastjgame.test;

import com.google.common.collect.Maps;
import com.wjybxx.fastjgame.utils.ClassScanner;
import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 不专业的map性能测试
 * 结论： {@link Int2ObjectMap} >  {@link IdentityHashMap} > {@link java.util.HashMap}
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/12/10
 * github - https://github.com/hl845740757
 */
public class MapPerformanceTest {


    public static void main(String[] args) {
        final Set<Class<?>> allClass = ClassScanner.findAllClass("org");

        // 预热
        putAllToIdentityMap(allClass, 10);
        putAllToHashMap(allClass, 10);
        putAllToIntMap(allClass, 10);

        // 开始
        putAllToIdentityMap(allClass, 100);
        putAllToHashMap(allClass, 100);
        putAllToIntMap(allClass, 100);
    }

    private static void putAllToIdentityMap(final Set<Class<?>> allClass, final int maxLoop) {
        System.out.println("\nmaxLoop: " + maxLoop);
        final IdentityHashMap<Class<?>, Object> map = new IdentityHashMap<>(allClass.size());
        final long startTimeMs = System.currentTimeMillis();
        for (int loop = 0; loop < maxLoop; loop++) {
            for (Class<?> clazz : allClass) {
                map.put(clazz, clazz);
            }
        }
        System.out.println("cost time ms: " + (System.currentTimeMillis() - startTimeMs));
        System.out.println(map.size());
    }

    private static void putAllToHashMap(final Set<Class<?>> allClass, final int maxLoop) {
        System.out.println("\nmaxLoop: " + maxLoop);
        final Map<Class<?>, Object> map = Maps.newHashMapWithExpectedSize(allClass.size());
        final long startTimeMs = System.currentTimeMillis();
        for (int loop = 0; loop < maxLoop; loop++) {
            for (Class<?> clazz : allClass) {
                map.put(clazz, clazz);
            }
        }
        System.out.println("cost time ms: " + (System.currentTimeMillis() - startTimeMs));
        System.out.println(map.size());
    }

    private static void putAllToIntMap(final Set<Class<?>> allClass, final int maxLoop) {
        System.out.println("\nmaxLoop: " + maxLoop);
        final Int2ObjectMap<Object> map = new Int2ObjectOpenHashMap<>(HashCommon.arraySize(allClass.size(), 0.75f));
        final long startTimeMs = System.currentTimeMillis();
        for (int loop = 0; loop < maxLoop; loop++) {
            int i = 0;
            for (Class<?> clazz : allClass) {
                map.put(i++, clazz);
            }
        }
        System.out.println("cost time ms: " + (System.currentTimeMillis() - startTimeMs));
        System.out.println(map.size());
    }

}
