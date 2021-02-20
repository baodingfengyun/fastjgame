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

package com.wjybxx.fastjgame.util;

import com.wjybxx.fastjgame.util.function.*;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongCollection;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import it.unimi.dsi.fastutil.shorts.ShortCollection;
import it.unimi.dsi.fastutil.shorts.ShortConsumer;
import it.unimi.dsi.fastutil.shorts.ShortIterator;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;
import java.util.function.LongConsumer;
import java.util.function.LongPredicate;

/**
 * 针对fastUtil集合的帮助类。
 * 注意：
 * 1. 在fastUtil的基本类型map中，key-value是分开存放的，因此如果以{@link Map.Entry}的形式遍历会产生大量的对象！会完全失去它的优势！
 * 如果遍历次数很频繁，请使用{@code fastIterator}进行优化。
 * 2. fastUtil的map/set等集合，构造方法传的是期望的元素数量和负载系数{@code (expected, loadFactor)}，而不是初始容量和负载系数。
 * 不必费脑子计算需要多少的初始容量。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/9 12:36
 * github - https://github.com/hl845740757
 */
public class FastCollectionsUtils {

    private FastCollectionsUtils() {

    }

    // 使用fastIterator可避免创建大量的entry对象
    public static <V> ObjectIterator<Long2ObjectMap.Entry<V>> fastIterator(Long2ObjectMap<V> map) {
        final ObjectSet<Long2ObjectMap.Entry<V>> entrySet = map.long2ObjectEntrySet();
        if (entrySet instanceof Long2ObjectMap.FastEntrySet) {
            return ((Long2ObjectMap.FastEntrySet<V>) entrySet).fastIterator();
        } else {
            return entrySet.iterator();
        }
    }

    public static <V> ObjectIterator<Int2ObjectMap.Entry<V>> fastIterator(Int2ObjectMap<V> map) {
        final ObjectSet<Int2ObjectMap.Entry<V>> entrySet = map.int2ObjectEntrySet();
        if (entrySet instanceof Int2ObjectMap.FastEntrySet) {
            return ((Int2ObjectMap.FastEntrySet<V>) entrySet).fastIterator();
        } else {
            return entrySet.iterator();
        }
    }

    public static <V> ObjectIterator<Short2ObjectMap.Entry<V>> fastIterator(Short2ObjectMap<V> map) {
        final ObjectSet<Short2ObjectMap.Entry<V>> entrySet = map.short2ObjectEntrySet();
        if (entrySet instanceof Short2ObjectMap.FastEntrySet) {
            return ((Short2ObjectMap.FastEntrySet<V>) entrySet).fastIterator();
        } else {
            return entrySet.iterator();
        }
    }

    /**
     * 移除map中符合条件的元素，并对删除的元素执行后续的操作。
     *
     * @param map       必须是可修改的map
     * @param predicate 过滤条件，为真的删除
     * @param then      元素删除之后执行的逻辑
     * @param <V>       the type of value
     * @return 删除的元素数量
     */
    public static <V> int removeIfAndThen(final Long2ObjectMap<V> map, final LongObjPredicate<? super V> predicate, @Nullable LongObjConsumer<V> then) {
        if (map.size() == 0) {
            return 0;
        }

        int removed = 0;
        Long2ObjectMap.Entry<V> entry;
        long k;
        V v;
        for (ObjectIterator<Long2ObjectMap.Entry<V>> itr = fastIterator(map); itr.hasNext(); ) {
            entry = itr.next();
            k = entry.getLongKey();
            v = entry.getValue();
            if (predicate.test(k, v)) {
                itr.remove();
                removed++;
                if (then != null) {
                    then.accept(k, v);
                }
            }
        }
        return removed;
    }

    /**
     * 移除map中符合条件的元素，并对删除的元素执行后续的操作
     *
     * @param map       必须是可修改的map
     * @param predicate 过滤条件，为真的删除
     * @param then      元素删除之后执行的逻辑
     * @param <V>       the type of value
     * @return 删除的元素数量
     */
    public static <V> int removeIfAndThen(final Int2ObjectMap<V> map, final IntObjPredicate<? super V> predicate, @Nullable IntObjConsumer<V> then) {
        if (map.size() == 0) {
            return 0;
        }

        int removed = 0;
        Int2ObjectMap.Entry<V> entry;
        int k;
        V v;
        for (ObjectIterator<Int2ObjectMap.Entry<V>> itr = fastIterator(map); itr.hasNext(); ) {
            entry = itr.next();
            k = entry.getIntKey();
            v = entry.getValue();
            if (predicate.test(k, v)) {
                itr.remove();
                removed++;
                if (then != null) {
                    then.accept(k, v);
                }
            }
        }
        return removed;
    }

    /**
     * 移除map中符合条件的元素，并对删除的元素执行后续的操作
     *
     * @param map       必须是可修改的map
     * @param predicate 过滤条件，为真的删除
     * @param then      元素删除之后执行的逻辑
     * @param <V>       the type of value
     * @return 删除的元素数量
     */
    public static <V> int removeIfAndThen(final Short2ObjectMap<V> map, final ShortObjPredicate<? super V> predicate, @Nullable ShortObjConsumer<V> then) {
        if (map.size() == 0) {
            return 0;
        }

        int removed = 0;
        Short2ObjectMap.Entry<V> entry;
        short k;
        V v;
        for (ObjectIterator<Short2ObjectMap.Entry<V>> itr = fastIterator(map); itr.hasNext(); ) {
            entry = itr.next();
            k = entry.getShortKey();
            v = entry.getValue();
            if (predicate.test(k, v)) {
                itr.remove();
                removed++;
                if (then != null) {
                    then.accept(k, v);
                }
            }
        }
        return removed;
    }

    /**
     * 移除map中符合条件的元素，并对删除的元素执行后续的操作。
     *
     * @param collection 必须是可修改的集合
     * @param predicate  过滤条件，为真的删除
     * @param then       元素删除之后执行的逻辑
     * @return 删除的元素数量
     */
    public static int removeIfAndThen(LongCollection collection, LongPredicate predicate, @Nullable LongConsumer then) {
        if (collection.size() == 0) {
            return 0;
        }

        int removed = 0;
        long value;
        for (LongIterator itr = collection.iterator(); itr.hasNext(); ) {
            value = itr.nextLong();
            if (predicate.test(value)) {
                itr.remove();
                removed++;
                if (then != null) {
                    then.accept(value);
                }
            }
        }
        return removed;
    }

    /**
     * 移除map中符合条件的元素，并对删除的元素执行后续的操作。
     *
     * @param collection 必须是可修改的集合
     * @param predicate  过滤条件，为真的删除
     * @param then       元素删除之后执行的逻辑
     * @return 删除的元素数量
     */
    public static int removeIfAndThen(IntCollection collection, IntPredicate predicate, @Nullable IntConsumer then) {
        if (collection.size() == 0) {
            return 0;
        }

        int removed = 0;
        int value;
        for (final IntIterator itr = collection.iterator(); itr.hasNext(); ) {
            value = itr.nextInt();
            if (predicate.test(value)) {
                itr.remove();
                removed++;
                if (then != null) {
                    then.accept(value);
                }
            }
        }
        return removed;
    }

    /**
     * 移除map中符合条件的元素，并对删除的元素执行后续的操作。
     *
     * @param collection 必须是可修改的集合
     * @param predicate  过滤条件，为真的删除
     * @param then       元素删除之后执行的逻辑
     * @return 删除的元素数量
     */
    public static int removeIfAndThen(ShortCollection collection, ShortPredicate predicate, @Nullable ShortConsumer then) {
        if (collection.size() == 0) {
            return 0;
        }

        int removed = 0;
        short value;
        for (final ShortIterator itr = collection.iterator(); itr.hasNext(); ) {
            value = itr.nextShort();
            if (predicate.test(value)) {
                itr.remove();
                removed++;
                if (then != null) {
                    then.accept(value);
                }
            }
        }
        return removed;
    }

    // region 要求制定键值不存在 或 存在

    public static <V> void requireNotContains(Int2ObjectMap<V> map, int key, String msg) {
        if (map.containsKey(key)) {
            throw new IllegalArgumentException(msg + " " + key + " is duplicate");
        }
    }

    public static <V> void requireContains(Int2ObjectMap<V> map, int key, String msg) {
        if (!map.containsKey(key)) {
            throw new IllegalArgumentException(msg + " " + key + " is not existent");
        }
    }

    public static <V> void requireNotContains(Long2ObjectMap<V> map, long key, String msg) {
        if (map.containsKey(key)) {
            throw new IllegalArgumentException(msg + " " + key + " is duplicate");
        }
    }

    public static <V> void requireContains(Long2ObjectMap<V> map, long key, String msg) {
        if (!map.containsKey(key)) {
            throw new IllegalArgumentException(msg + " " + key + " is not existent");
        }
    }

    public static <V> void requireNotContains(Short2ObjectMap<V> map, short key, String msg) {
        if (map.containsKey(key)) {
            throw new IllegalArgumentException(msg + " " + key + " is duplicate");
        }
    }

    public static <V> void requireContains(Short2ObjectMap<V> map, short key, String msg) {
        if (!map.containsKey(key)) {
            throw new IllegalArgumentException(msg + " " + key + " is not existent");
        }
    }

    // endregion
}
