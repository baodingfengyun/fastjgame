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

package com.wjybxx.fastjgame.utils;

import com.wjybxx.fastjgame.function.MapConstructor;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * 集合帮助类
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
     * 保留entry对象有潜在风险
     *
     * @param collection 可修改的集合
     * @param filter     什么样的元素需要删除
     * @param then       元素删除之后执行逻辑
     * @param <E>        元素的类型。注意：不可以是{@link Map.Entry}
     * @return 返回删除成功的元素
     */
    public static <E> int removeIfAndThen(Collection<E> collection, Predicate<E> filter, Consumer<E> then) {
        if (collection.size() == 0) {
            return 0;
        }
        Iterator<E> iterator = collection.iterator();
        E e;
        int removeNum = 0;
        while (iterator.hasNext()) {
            e = iterator.next();
            if (filter.test(e)) {
                iterator.remove();
                removeNum++;
                then.accept(e);
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
     * @param <K>       the type of key
     * @param <V>       the type of value
     * @return 删除的元素数量
     */
    public static <K, V> int removeIfAndThen(final Map<K, V> map, final BiPredicate<? super K, ? super V> predicate, BiConsumer<K, V> then) {
        if (map.size() == 0){
            return 0;
        }

        int removeNum = 0;
        // entry在调用remove之后不可再访问,因此需要将key-value保存下来
        Map.Entry<K, V> kvEntry;
        K k;
        V v;
        Iterator<Map.Entry<K, V>> itr = map.entrySet().iterator();
        while (itr.hasNext()) {
            kvEntry = itr.next();
            k = kvEntry.getKey();
            v = kvEntry.getValue();

            if (predicate.test(k, v)) {
                itr.remove();
                removeNum++;
                then.accept(k, v);
            }
        }
        return removeNum;
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
        return removeIfAndThen(map, predicate, FunctionUtils.emptyBiConsumer());
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
        Iterator<E> iterator = collection.iterator();
        while (iterator.hasNext()) {
            if (predicate.test(iterator.next())) {
                iterator.remove();
                return true;
            }
        }
        return false;
    }

    // -------------------------------------------------- 分割线 ---------------------------------------------------

    /**
     * 将并发队列中的元素全部收集到指定集合中。
     * 解决的问题是什么？
     * 如果遍历之后调用clear()，clear删除的元素可能比你遍历的并不一致。 可参考竞态条件之 '先检查再执行'。
     * {@link BlockingQueue}支持{@code drainTo(Collection)}，但是{@link ConcurrentLinkedQueue}并不支持。
     *
     * @param concurrentLinkedQueue 并发队列
     * @param out                   目标list，并发队列弹出的元素会放入该list
     * @param <E>                   元素的类型
     * @return the number of elements transferred
     */
    public static <E> int drainQueue(ConcurrentLinkedQueue<E> concurrentLinkedQueue, Collection<E> out) {
        E e;
        int num = 0;
        while ((e = concurrentLinkedQueue.poll()) != null) {
            out.add(e);
            num++;
        }
        return num;
    }

    /**
     * 删除阻塞队列当前所有元素，并添加到指定集合中。
     * 如果在这期间没有线程向blockingQueue中添加元素的话，返回之后blockingQueue中的size == 0
     *
     * <h3>drainTo方法介绍</h3>
     * Removes all available elements from this queue and adds them
     * to the given collection.  This operation may be more
     * efficient than repeatedly polling this queue.  A failure
     * encountered while attempting to add elements to
     * collection {@code c} may result in elements being in neither,
     * either or both collections when the associated exception is
     * thrown.  Attempts to drain a queue to itself result in
     * {@code IllegalArgumentException}. Further, the behavior of
     * this operation is undefined if the specified collection is
     * modified while the operation is in progress.
     * <p>
     * 将阻塞队列中的所有可用元素全部收集到指定集合中。
     * 注意：drainTo方法返回之后，不代表{@link BlockingQueue}中没有元素！
     *
     * <h3>available elements</h3>
     * 可用元素：是指队列中可通过poll删除的元素。
     *
     * <h3>special queue</h3>
     * 某些队列中的元素只有在满足一定条件之后，才能从队列中poll，因此该方法返回之后，队列中仍然可能有元素存在。
     *
     * <b> 该方法参考自JDK {@link java.util.concurrent.ThreadPoolExecutor} </b>
     * Drains the task queue into a new list, normally using
     * drainTo. But if the queue is a DelayQueue or any other kind of
     * queue for which poll or drainTo may fail to remove some
     * elements, it deletes them one by one.
     *
     * @param blockingQueue 期望删除元素的阻塞队列
     * @param out           输出集
     * @param <E>           the type of element
     * @param <T>           the type of collection
     * @return the number of elements transferred
     */
    public static <E, T extends Collection<E>> int drainQueue(BlockingQueue<E> blockingQueue, T out) {
        final int preSize = out.size();
        blockingQueue.drainTo(out);
        // 剩下的强制删除
        forceDrain(blockingQueue, out);
        return out.size() - preSize;
    }

    private static <E, T extends Collection<E>> void forceDrain(Queue<E> queue, T out) {
        if (!queue.isEmpty()) {
            for (Object obj : queue.toArray()) {
                @SuppressWarnings("unchecked")
                E e = (E) obj;
                if (queue.remove(e)) {
                    // 必须要移除成功才能添加！
                    out.add(e);
                }
            }
        }
    }

    /**
     * 删除队列中的所有元素并添加到指定队列中。
     *
     * @param queue 期望删除元素的队列
     * @param out   输出集
     * @param <E>   the type of element
     * @param <T>   the type of collection
     * @return the number of elements transferred
     */
    public static <E, T extends Collection<E>> int drainQueue(Queue<E> queue, T out) {
        if (queue instanceof BlockingQueue) {
            return drainQueue((BlockingQueue<E>) queue, out);
        }
        if (queue instanceof ConcurrentLinkedQueue) {
            return drainQueue((ConcurrentLinkedQueue<E>) queue, out);
        }
        final int preSize = out.size();
        // 强制删
        forceDrain(queue, out);
        return out.size() - preSize;
    }


    /**
     * 在blockingQueue中使用poll方式寻找某个元素
     *
     * @param blockingQueue 元素队列，元素中除了期望的一个元素以外都无用
     * @param matcher       匹配函数
     * @param <E>           the type of element
     * @return 如果找不到则返回null
     */
    public static <E> E findElementWithPoll(BlockingQueue<E> blockingQueue, Predicate<E> matcher) {
        return findElementWithPollImp(blockingQueue, matcher);
    }

    /**
     * 在concurrentLinkedQueue中使用poll方式寻找某个元素
     *
     * @param concurrentLinkedQueue 元素队列，元素中除了期望的一个元素以外都无用
     * @param matcher               匹配函数
     * @param <E>                   the type of element
     * @return 如果找不到则返回null
     */
    public static <E> E findElementWithPoll(ConcurrentLinkedQueue<E> concurrentLinkedQueue, Predicate<E> matcher) {
        return findElementWithPollImp(concurrentLinkedQueue, matcher);
    }

    /**
     * 使用poll方式在队列中寻找元素的真正实现，之所以不暴露，是因为主要是用于解决并发队列问题的。
     *
     * @param queue   元素队列，元素中除了期望的一个元素以外都无用
     * @param matcher 匹配函数
     * @param <E>     the type of element
     * @return 如果找不到则返回null，其实函数式方式可以返回 {@link Optional}
     */
    private static <E> E findElementWithPollImp(Queue<E> queue, Predicate<E> matcher) {
        E e;
        while ((e = queue.poll()) != null) {
            if (matcher.test(e)) {
                return e;
            }
        }
        return null;
    }

    /**
     * 使用poll方式等待blockingQueue中出现某个元素
     *
     * @param blockingQueue 元素队列，元素中除了期望的一个元素以外都无用
     * @param matcher       匹配函数
     * @param maxWaitTime   最大等待时间 毫秒
     * @param <E>           the type of element
     * @return 未在限定时间内等到期望的元素，则返回null
     */
    public static <E> E waitElementWithPoll(BlockingQueue<E> blockingQueue, Predicate<E> matcher, int maxWaitTime) {
        boolean interrupted = Thread.interrupted();
        long endTime = System.currentTimeMillis() + maxWaitTime;
        try {
            for (long remainTime = maxWaitTime; remainTime > 0; remainTime = endTime - System.currentTimeMillis()) {
                try {
                    E e = blockingQueue.poll(remainTime, TimeUnit.MILLISECONDS);
                    // 超时
                    if (null == e) {
                        return null;
                    }
                    // 期望的元素
                    if (matcher.test(e)) {
                        return e;
                    }
                } catch (InterruptedException e) {
                    // 收到了中断请求，我们先不处理它，但是要将它存储下来，继续尝试
                    interrupted = true;
                }
            }
            // 超时
            return null;
        } finally {
            // 返回前恢复中断状态
            ConcurrentUtils.recoveryInterrupted(interrupted);
        }
    }

    public static <K> void requireNotContains(Map<K, ?> map, K k, String name) {
        if (map.containsKey(k)) {
            throw new IllegalArgumentException("duplicate " + name + " " + k);
        }
    }

    /**
     * 创建足够容量的Map，可减少扩容次数，适合用在能估算最大容量的时候;
     *
     * @param constructor  map的构造器函数
     * @param initCapacity 初始容量 大于0有效
     * @param <K>          key的类型
     * @param <V>          value的类型
     * @return M
     */
    public static <K, V, M extends Map<K, V>> M newEnoughCapacityMap(MapConstructor<M> constructor, int initCapacity) {
        return initCapacity > 0 ? constructor.newMap(initCapacity, 1) : constructor.newMap(16, 0.75f);
    }

    /**
     * 创建足够容量的HashMap，可减少扩容次数，适合用在能估算最大容量的时候;
     *
     * @param initCapacity 初始容量 大于0有效
     * @param <K>          key的类型
     * @param <V>          value的类型
     */
    public static <K, V> HashMap<K, V> newEnoughCapacityHashMap(int initCapacity) {
        return newEnoughCapacityMap(HashMap::new, initCapacity);
    }

    /**
     * 创建足够容量的LinkecHashMap，可减少扩容次数，适合用在能估算最大容量的时候;
     *
     * @param initCapacity 初始容量 大于0有效
     * @param <K>          key的类型
     * @param <V>          value的类型
     * @return LinkedHashMap
     */
    public static <K, V> LinkedHashMap<K, V> newEnoughCapacityLinkedHashMap(int initCapacity) {
        return newEnoughCapacityMap(LinkedHashMap::new, initCapacity);
    }

    /**
     * 创建足够容量的HashSet，可减少扩容次数，适合用在能估算最大容量的时候;
     *
     * @param initCapacity 初始容量 大于0有效
     * @param <E> the type of element
     * @return HashSet
     */
    public static <E> HashSet<E> newEnoughCapacityHashSet(int initCapacity) {
        if (initCapacity > 0) {
            return new HashSet<>(initCapacity, 1);
        } else {
            return new HashSet<>();
        }
    }

    /**
     * 创建足够容量的LinkecHashSet，适合用在能估算最大容量的时候;
     *
     * @param initCapacity 初始容量 大于0有效
     * @return LinkedHashSet
     */
    public static <E> LinkedHashSet<E> newEnoughCapacityLinkedHashSet(int initCapacity) {
        if (initCapacity > 0) {
            return new LinkedHashSet<>(initCapacity, 1);
        } else {
            return new LinkedHashSet<>();
        }
    }

    /**
     * 将任务呀队列中的元素拉取到指定缓存队列
     *
     * @param taskQueue   任务队列
     * @param cacheQueue  缓存队列
     * @param maxElements 最多拉取多少数据到缓存队列
     * @param <E>         元素类型
     * @return 弹出的元素格式
     */
    public static <E> int pollTo(Queue<E> taskQueue, Collection<E> cacheQueue, int maxElements) {
        if (maxElements == 0) {
            throw new IllegalArgumentException("maxElements");
        }
        E e;
        int elements = 0;
        while ((e = taskQueue.poll()) != null) {
            cacheQueue.add(e);
            elements++;

            if (elements >= maxElements) {
                break;
            }
        }
        return elements;
    }
}