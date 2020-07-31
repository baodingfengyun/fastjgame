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
import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import it.unimi.dsi.fastutil.shorts.ShortCollection;
import it.unimi.dsi.fastutil.shorts.ShortConsumer;
import it.unimi.dsi.fastutil.shorts.ShortIterator;

import java.util.Map;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;
import java.util.function.LongConsumer;
import java.util.function.LongPredicate;

/**
 * 针对fastUtil集合的帮助类。
 * 注意：
 * 1. fastUtil的一个大坑！由于fastUtil的map的key-value是分开存放的，因此如果以{@link Map.Entry}的形式遍历会产生大量的对象！！！
 * 会完全失去它的优势。
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

    /**
     * 移除map中符合条件的元素，并对删除的元素执行后续的操作。
     *
     * @param map       必须是可修改的map
     * @param predicate 过滤条件，为真的删除
     * @param then      元素删除之后执行的逻辑
     * @param <V>       the type of value
     * @return 删除的元素数量
     */
    public static <V> int removeIfAndThen(final Long2ObjectMap<V> map, final LongObjPredicate<? super V> predicate, LongObjConsumer<V> then) {
        if (map.size() == 0) {
            return 0;
        }

        ObjectIterator<Long2ObjectMap.Entry<V>> itr = map.long2ObjectEntrySet().iterator();
        int removeNum = 0;
        Long2ObjectMap.Entry<V> entry;
        long k;
        V v;
        while (itr.hasNext()) {
            entry = itr.next();
            k = entry.getLongKey();
            v = entry.getValue();
            if (predicate.test(k, v)) {
                itr.remove();
                removeNum++;
                then.accept(k, v);
            }
        }
        return removeNum;
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
    public static <V> int removeIfAndThen(final Int2ObjectMap<V> map, final IntObjPredicate<? super V> predicate, IntObjConsumer<V> then) {
        if (map.size() == 0) {
            return 0;
        }

        ObjectIterator<Int2ObjectMap.Entry<V>> itr = map.int2ObjectEntrySet().iterator();
        int removeNum = 0;
        Int2ObjectMap.Entry<V> entry;
        int k;
        V v;
        while (itr.hasNext()) {
            entry = itr.next();
            k = entry.getIntKey();
            v = entry.getValue();
            if (predicate.test(k, v)) {
                itr.remove();
                removeNum++;
                then.accept(k, v);
            }
        }
        return removeNum;
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
    public static <V> int removeIfAndThen(final Short2ObjectMap<V> map, final ShortObjPredicate<? super V> predicate, ShortObjConsumer<V> then) {
        if (map.size() == 0) {
            return 0;
        }

        ObjectIterator<Short2ObjectMap.Entry<V>> itr = map.short2ObjectEntrySet().iterator();
        int removeNum = 0;
        Short2ObjectMap.Entry<V> entry;
        short k;
        V v;
        while (itr.hasNext()) {
            entry = itr.next();
            k = entry.getShortKey();
            v = entry.getValue();
            if (predicate.test(k, v)) {
                itr.remove();
                removeNum++;
                then.accept(k, v);
            }
        }
        return removeNum;
    }

    /**
     * 移除map中符合条件的元素，并对删除的元素执行后续的操作。
     *
     * @param collection 必须是可修改的集合
     * @param predicate  过滤条件，为真的删除
     * @param then       元素删除之后执行的逻辑
     * @return 删除的元素数量
     */
    public static int removeIfAndThen(LongCollection collection, LongPredicate predicate, LongConsumer then) {
        if (collection.size() == 0) {
            return 0;
        }
        final LongIterator itr = collection.iterator();
        long value;
        int removeNum = 0;
        while (itr.hasNext()) {
            value = itr.nextLong();
            if (predicate.test(value)) {
                itr.remove();
                removeNum++;
                then.accept(value);
            }
        }
        return removeNum;
    }

    /**
     * 移除map中符合条件的元素，并对删除的元素执行后续的操作。
     *
     * @param collection 必须是可修改的集合
     * @param predicate  过滤条件，为真的删除
     * @param then       元素删除之后执行的逻辑
     * @return 删除的元素数量
     */
    public static int removeIfAndThen(IntCollection collection, IntPredicate predicate, IntConsumer then) {
        if (collection.size() == 0) {
            return 0;
        }
        final IntIterator itr = collection.iterator();
        int value;
        int removeNum = 0;
        while (itr.hasNext()) {
            value = itr.nextInt();
            if (predicate.test(value)) {
                itr.remove();
                removeNum++;
                then.accept(value);
            }
        }
        return removeNum;
    }

    /**
     * 移除map中符合条件的元素，并对删除的元素执行后续的操作。
     *
     * @param collection 必须是可修改的集合
     * @param predicate  过滤条件，为真的删除
     * @param then       元素删除之后执行的逻辑
     * @return 删除的元素数量
     */
    public static int removeIfAndThen(ShortCollection collection, ShortPredicate predicate, ShortConsumer then) {
        if (collection.size() == 0) {
            return 0;
        }
        final ShortIterator itr = collection.iterator();
        short value;
        int removeNum = 0;
        while (itr.hasNext()) {
            value = itr.nextShort();
            if (predicate.test(value)) {
                itr.remove();
                removeNum++;
                then.accept(value);
            }
        }
        return removeNum;
    }

    // region 要求制定键值不存在 或 存在

    public static <V> void requireNotContains(Int2ObjectMap<V> map, int key, String msg) {
        if (map.containsKey(key)) {
            throw new IllegalArgumentException("duplicate " + msg + "-" + key);
        }
    }

    public static <V> void requireContains(Int2ObjectMap<V> map, int key, String msg) {
        if (!map.containsKey(key)) {
            throw new IllegalArgumentException("nonexistent " + msg + "-" + key);
        }
    }

    public static <V> void requireNotContains(Long2ObjectMap<V> map, long key, String msg) {
        if (map.containsKey(key)) {
            throw new IllegalArgumentException("duplicate " + msg + "-" + key);
        }
    }

    public static <V> void requireContains(Long2ObjectMap<V> map, long key, String msg) {
        if (!map.containsKey(key)) {
            throw new IllegalArgumentException("nonexistent " + msg + "-" + key);
        }
    }

    public static <V> void requireNotContains(Short2ObjectMap<V> map, short key, String msg) {
        if (map.containsKey(key)) {
            throw new IllegalArgumentException("duplicate " + msg + "-" + key);
        }
    }

    public static <V> void requireContains(Short2ObjectMap<V> map, short key, String msg) {
        if (!map.containsKey(key)) {
            throw new IllegalArgumentException("nonexistent " + msg + "-" + key);
        }
    }

    // endregion
}
