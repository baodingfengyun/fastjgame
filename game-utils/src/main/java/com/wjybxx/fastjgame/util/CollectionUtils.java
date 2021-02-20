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

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * 集合帮助类
 * 注意： JDK的map/set等集合，构造方法传递的是初始容量和负载系数，必须费脑子计算到底多少初始容量恰好合适。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/4/29 10:19
 * github - https://github.com/hl845740757
 */
public final class CollectionUtils {

    private CollectionUtils() {

    }

    /**
     * 删除集合中满足条件的元素，并对删除的元素执行后续逻辑。
     * 建议map使用{@link #removeIfAndThen(Map, BiPredicate, BiConsumer)}进行删除
     *
     * @param collection 可修改的集合
     * @param filter     什么样的元素需要删除
     * @param then       元素删除之后执行逻辑
     * @param <E>        元素的类型。注意：不可以是{@link Map.Entry}
     * @return 返回删除成功的元素
     */
    public static <E> int removeIfAndThen(final Collection<E> collection, final Predicate<E> filter, @Nullable final Consumer<E> then) {
        if (collection.size() == 0) {
            return 0;
        }

        int removed = 0;
        E e;
        for (Iterator<E> iterator = collection.iterator(); iterator.hasNext(); ) {
            e = iterator.next();
            if (filter.test(e)) {
                iterator.remove();
                removed++;
                if (null != then) {
                    then.accept(e);
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
     * @param <K>       the type of key
     * @param <V>       the type of value
     * @return 删除的元素数量
     */
    public static <K, V> int removeIfAndThen(final Map<K, V> map, final BiPredicate<? super K, ? super V> predicate,
                                             @Nullable final BiConsumer<K, V> then) {
        if (map.size() == 0) {
            return 0;
        }

        int removed = 0;
        // entry在调用remove之后不可再访问,因此需要将key-value保存下来
        Map.Entry<K, V> kvEntry;
        K k;
        V v;
        for (Iterator<Map.Entry<K, V>> itr = map.entrySet().iterator(); itr.hasNext(); ) {
            kvEntry = itr.next();
            k = kvEntry.getKey();
            v = kvEntry.getValue();
            if (predicate.test(k, v)) {
                itr.remove();
                removed++;
                if (null != then) {
                    then.accept(k, v);
                }
            }
        }
        return removed;
    }

    /**
     * 删除掉map中满足条件的
     *
     * @param map       必须是可修改的map
     * @param predicate 过滤条件，为真的删除
     * @param <K>       the type of key
     * @param <V>       the type of value
     * @return 删除的元素数量
     */
    public static <K, V> int removeIf(final Map<K, V> map, final BiPredicate<? super K, ? super V> predicate) {
        return removeIfAndThen(map, predicate, null);
    }

    /**
     * 移除集合中第一个匹配的元素
     *
     * @param collection 可修改的集合
     * @param predicate  删除条件，为true的删除。
     * @param <E>        集合中的元素类型
     * @return 是否成功删除了一个元素
     */
    public static <E> boolean removeFirstMatch(Collection<E> collection, Predicate<? super E> predicate) {
        if (collection.size() == 0) {
            return false;
        }
        for (Iterator<E> itr = collection.iterator(); itr.hasNext(); ) {
            if (predicate.test(itr.next())) {
                itr.remove();
                return true;
            }
        }
        return false;
    }

    //////////////////////////////////////////////////////////////////////////////////////

    public static <K, V> void requireNotContains(Map<K, V> map, K key, String property) {
        if (map.containsKey(key)) {
            throwDuplicateException(key, property);
        }
    }

    static <K> void throwDuplicateException(K key, String property) {
        throw new IllegalArgumentException(property + " " + key + " is duplicate");
    }

    public static <K, V> void requireContains(Map<K, V> map, K key, String property) {
        if (map.containsKey(key)) {
            throwNotContainsException(key, property);
        }
    }

    static <K> void throwNotContainsException(K key, String property) {
        throw new IllegalArgumentException(property + " " + key + " is not existent");
    }

    //////////////////////////////////////////////////////////////////////////////////////
}