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

package com.wjybxx.fastjgame.redis;

import redis.clients.jedis.ListPosition;
import redis.clients.jedis.Tuple;
import redis.clients.jedis.params.ZAddParams;
import redis.clients.jedis.params.ZIncrByParams;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 封装{@link redis.clients.jedis.commands.RedisPipeline}，提供回调等支持。
 * 实在是佩服jedis的作者。
 * 咱们按需添加就好。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/12/12
 * github - https://github.com/hl845740757
 */
public interface RedisPipeline {

    // region string

    RedisResponse<Long> incr(String key);

    RedisResponse<Long> incrBy(String key, long increment);

    // endregion

    // region list

    RedisResponse<String> lindex(String key, long index);

    RedisResponse<Long> linsert(String key, ListPosition where, String pivot, String value);

    /**
     * left pop
     * 移除并且返回 key 对应的 list 的第一个元素。
     */
    RedisResponse<String> lpop(String key);

    /**
     * left push
     * <p>
     * 将所有指定的值插入到存于 key 的列表的头部。
     * 如果 key 不存在，那么在进行 push 操作前会创建一个空列表。
     * 如果 key 对应的值不是一个 list 的话，那么会返回一个错误。
     * <p>
     * 可以使用一个命令把多个元素放入列表，只需在命令末尾加上多个指定的参数。
     * 元素是从最左端的到最右端的、一个接一个被插入到 list 的头部。
     * 所以对于这个命令例子 LPUSH mylist a b c，返回的列表是 c 为第一个元素， b 为第二个元素， a 为第三个元素。
     */
    RedisResponse<Long> lpush(String key, String... string);

    /**
     * left pushx
     * 只有当 key 已经存在并且存着一个 list 的时候，在这个 key 下面的 list 的头部插入 value。
     * 与 LPUSH 相反，当 key 不存在的时候不会进行任何操作。
     */
    RedisResponse<Long> lpushx(String key, String... string);

    /**
     * right pop
     * 移除并返回存于 key 的 list 的最后一个元素。
     */
    RedisResponse<String> rpop(String key);

    /**
     * right push
     * 向存于 key 的列表的尾部插入所有指定的值。
     * 如果 key 不存在，那么会创建一个空的列表然后再进行 push 操作。
     * 当 key 保存的不是一个列表时，那么会返回一个错误。
     * <p>
     * 可以使用一个命令把多个元素放入队列，只需要在命令后面指定多个参数。
     * 元素是从左到右一个接一个从列表尾部插入。
     * 比如命令 RPUSH mylist a b c 会返回一个列表，其第一个元素是 a ，第二个元素是 b ，第三个元素是 c。
     */
    RedisResponse<Long> rpush(String key, String... string);

    /**
     * right pushx
     * 当且仅当 key 存在并且是一个列表时，将值 value 插入到列表 key 的表尾。
     * 和 RPUSH 命令相反, 当 key 不存在时，RPUSHX 命令什么也不做。
     */
    RedisResponse<Long> rpushx(String key, String... string);

    RedisResponse<List<String>> lrange(String key, long start, long stop);

    RedisResponse<Long> lrem(String key, long count, String value);

    RedisResponse<String> lset(String key, long index, String value);

    RedisResponse<String> ltrim(String key, long start, long stop);

    RedisResponse<Long> llen(String key);

    // endregion

    // region hash
    RedisResponse<Boolean> hexists(String key, String field);

    RedisResponse<String> hget(String key, String field);

    RedisResponse<Long> hdel(String key, String... field);

    RedisResponse<Long> hincrBy(String key, String field, long value);

    RedisResponse<Long> hset(String key, String field, String value);

    RedisResponse<Long> hsetnx(String key, String field, String value);

    RedisResponse<List<String>> hmget(String key, String... fields);

    RedisResponse<String> hmset(String key, Map<String, String> hash);

    RedisResponse<List<String>> hvals(String key);

    RedisResponse<Long> hlen(String key);

    RedisResponse<Set<String>> hkeys(String key);

    RedisResponse<Map<String, String>> hgetAll(String key);

    // endregion

    // region zset
    // 不再建议使用redis做排行榜，使用自己实现的java-zset做排行榜更合适

    RedisResponse<Long> zadd(String key, double score, String member);

    RedisResponse<Long> zadd(String key, double score, String member, ZAddParams params);

    RedisResponse<Long> zadd(String key, Map<String, Double> scoreMembers);

    RedisResponse<Long> zadd(String key, Map<String, Double> scoreMembers, ZAddParams params);

    RedisResponse<Long> zcard(String key);

    RedisResponse<Long> zcount(String key, double min, double max);

    RedisResponse<Double> zincrby(String key, double increment, String member);

    RedisResponse<Double> zincrby(String key, double increment, String member, ZIncrByParams params);

    RedisResponse<Long> zrank(String key, String member);

    RedisResponse<Long> zrevrank(String key, String member);

    RedisResponse<Double> zscore(String key, String member);

    // 总是同时返回分数和member，其实更有意义，老的api着实不好用
    RedisResponse<Set<Tuple>> zrangeWithScores(String key, long start, long stop);

    RedisResponse<Set<Tuple>> zrangeByScoreWithScores(String key, double min, double max);

    RedisResponse<Set<Tuple>> zrangeByScoreWithScores(String key, double min, double max, int offset, int count);

    RedisResponse<Set<Tuple>> zrevrangeByScoreWithScores(String key, double max, double min);

    RedisResponse<Set<Tuple>> zrevrangeByScoreWithScores(String key, double max, double min, int offset, int count);

    RedisResponse<Set<Tuple>> zrevrangeWithScores(String key, long start, long stop);

    RedisResponse<Long> zrem(String key, String... members);

    RedisResponse<Long> zremrangeByRank(String key, long start, long stop);

    RedisResponse<Long> zremrangeByScore(String key, double min, double max);

    // endregion
}
