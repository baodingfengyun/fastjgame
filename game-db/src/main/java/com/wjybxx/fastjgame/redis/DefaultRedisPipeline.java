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

import com.wjybxx.fastjgame.concurrent.EventLoop;
import redis.clients.jedis.ListPosition;
import redis.clients.jedis.Tuple;
import redis.clients.jedis.params.ZAddParams;
import redis.clients.jedis.params.ZIncrByParams;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 它在用户线程{@link #appEventLoop}和redis线程{@link #redisEventLoop}之间架设了一个管道。
 * 还好有lambda表达式，不然真得累死。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/12/12
 * github - https://github.com/hl845740757
 */
public class DefaultRedisPipeline implements RedisPipeline {

    private final RedisEventLoop redisEventLoop;
    private final EventLoop appEventLoop;

    public DefaultRedisPipeline(RedisEventLoop redisEventLoop, EventLoop appEventLoop) {
        this.redisEventLoop = redisEventLoop;
        this.appEventLoop = appEventLoop;
    }

    @Override
    public RedisFuture<Long> incr(String key) {
        return redisEventLoop.enqueue(appEventLoop, pipeline -> pipeline.incr(key));
    }

    @Override
    public RedisFuture<Long> incrBy(String key, long increment) {
        return redisEventLoop.enqueue(appEventLoop, pipeline -> pipeline.incrBy(key, increment));
    }

    @Override
    public RedisFuture<String> lindex(String key, long index) {
        return redisEventLoop.enqueue(appEventLoop, pipeline -> pipeline.lindex(key, index));
    }

    @Override
    public RedisFuture<Long> linsert(String key, ListPosition where, String pivot, String value) {
        return redisEventLoop.enqueue(appEventLoop, pipeline -> pipeline.linsert(key, where, pivot, value));
    }

    @Override
    public RedisFuture<String> lpop(String key) {
        return redisEventLoop.enqueue(appEventLoop, pipeline -> pipeline.lpop(key));
    }

    @Override
    public RedisFuture<Long> lpush(String key, String... string) {
        return redisEventLoop.enqueue(appEventLoop, pipeline -> pipeline.lpush(key, string));
    }

    @Override
    public RedisFuture<Long> lpushx(String key, String... string) {
        return redisEventLoop.enqueue(appEventLoop, pipeline -> pipeline.lpushx(key, string));
    }

    @Override
    public RedisFuture<String> rpop(String key) {
        return redisEventLoop.enqueue(appEventLoop, pipeline -> pipeline.rpop(key));
    }

    @Override
    public RedisFuture<Long> rpush(String key, String... string) {
        return redisEventLoop.enqueue(appEventLoop, pipeline -> pipeline.rpush(key, string));
    }

    @Override
    public RedisFuture<Long> rpushx(String key, String... string) {
        return redisEventLoop.enqueue(appEventLoop, pipeline -> pipeline.rpushx(key, string));
    }

    @Override
    public RedisFuture<List<String>> lrange(String key, long start, long stop) {
        return redisEventLoop.enqueue(appEventLoop, pipeline -> pipeline.lrange(key, start, stop));
    }

    @Override
    public RedisFuture<Long> lrem(String key, long count, String value) {
        return redisEventLoop.enqueue(appEventLoop, pipeline -> pipeline.lrem(key, count, value));
    }

    @Override
    public RedisFuture<String> lset(String key, long index, String value) {
        return redisEventLoop.enqueue(appEventLoop, pipeline -> pipeline.lset(key, index, value));
    }

    @Override
    public RedisFuture<String> ltrim(String key, long start, long stop) {
        return redisEventLoop.enqueue(appEventLoop, pipeline -> pipeline.ltrim(key, start, stop));
    }

    @Override
    public RedisFuture<Long> llen(String key) {
        return redisEventLoop.enqueue(appEventLoop, pipeline -> pipeline.llen(key));
    }

    @Override
    public RedisFuture<Boolean> hexists(String key, String field) {
        return redisEventLoop.enqueue(appEventLoop, pipeline -> pipeline.hexists(key, field));
    }

    @Override
    public RedisFuture<String> hget(String key, String field) {
        return redisEventLoop.enqueue(appEventLoop, pipeline -> pipeline.hget(key, field));
    }

    @Override
    public RedisFuture<Long> hdel(String key, String... field) {
        return redisEventLoop.enqueue(appEventLoop, pipeline -> pipeline.hdel(key, field));
    }

    @Override
    public RedisFuture<Long> hincrBy(String key, String field, long value) {
        return redisEventLoop.enqueue(appEventLoop, pipeline -> pipeline.hincrBy(key, field, value));
    }

    @Override
    public RedisFuture<Long> hset(String key, String field, String value) {
        return redisEventLoop.enqueue(appEventLoop, pipeline -> pipeline.hset(key, field, value));
    }

    @Override
    public RedisFuture<Long> hsetnx(String key, String field, String value) {
        return redisEventLoop.enqueue(appEventLoop, pipeline -> pipeline.hsetnx(key, field, value));
    }

    @Override
    public RedisFuture<List<String>> hmget(String key, String... fields) {
        return redisEventLoop.enqueue(appEventLoop, pipeline -> pipeline.hmget(key, fields));
    }

    @Override
    public RedisFuture<String> hmset(String key, Map<String, String> hash) {
        return redisEventLoop.enqueue(appEventLoop, pipeline -> pipeline.hmset(key, hash));
    }

    @Override
    public RedisFuture<List<String>> hvals(String key) {
        return redisEventLoop.enqueue(appEventLoop, pipeline -> pipeline.hvals(key));
    }

    @Override
    public RedisFuture<Long> hlen(String key) {
        return redisEventLoop.enqueue(appEventLoop, pipeline -> pipeline.hlen(key));
    }

    @Override
    public RedisFuture<Set<String>> hkeys(String key) {
        return redisEventLoop.enqueue(appEventLoop, pipeline -> pipeline.hkeys(key));
    }

    @Override
    public RedisFuture<Map<String, String>> hgetAll(String key) {
        return redisEventLoop.enqueue(appEventLoop, pipeline -> pipeline.hgetAll(key));
    }

    @Override
    public RedisFuture<Long> zadd(String key, double score, String member) {
        return redisEventLoop.enqueue(appEventLoop, pipeline -> pipeline.zadd(key, score, member));
    }

    @Override
    public RedisFuture<Long> zadd(String key, double score, String member, ZAddParams params) {
        return redisEventLoop.enqueue(appEventLoop, pipeline -> pipeline.zadd(key, score, member, params));
    }

    @Override
    public RedisFuture<Long> zadd(String key, Map<String, Double> scoreMembers) {
        return redisEventLoop.enqueue(appEventLoop, pipeline -> pipeline.zadd(key, scoreMembers));
    }

    @Override
    public RedisFuture<Long> zadd(String key, Map<String, Double> scoreMembers, ZAddParams params) {
        return redisEventLoop.enqueue(appEventLoop, pipeline -> pipeline.zadd(key, scoreMembers, params));
    }

    @Override
    public RedisFuture<Long> zcard(String key) {
        return redisEventLoop.enqueue(appEventLoop, pipeline -> pipeline.zcard(key));
    }

    @Override
    public RedisFuture<Long> zcount(String key, double min, double max) {
        return redisEventLoop.enqueue(appEventLoop, pipeline -> pipeline.zcount(key, min, max));
    }

    @Override
    public RedisFuture<Double> zincrby(String key, double increment, String member) {
        return redisEventLoop.enqueue(appEventLoop, pipeline -> pipeline.zincrby(key, increment, member));
    }

    @Override
    public RedisFuture<Double> zincrby(String key, double increment, String member, ZIncrByParams params) {
        return redisEventLoop.enqueue(appEventLoop, pipeline -> pipeline.zincrby(key, increment, member, params));
    }

    @Override
    public RedisFuture<Long> zrank(String key, String member) {
        return redisEventLoop.enqueue(appEventLoop, pipeline -> pipeline.zrank(key, member));
    }

    @Override
    public RedisFuture<Long> zrevrank(String key, String member) {
        return redisEventLoop.enqueue(appEventLoop, pipeline -> pipeline.zrevrank(key, member));
    }

    @Override
    public RedisFuture<Double> zscore(String key, String member) {
        return redisEventLoop.enqueue(appEventLoop, pipeline -> pipeline.zscore(key, member));
    }

    @Override
    public RedisFuture<Set<Tuple>> zrangeWithScores(String key, long start, long stop) {
        return redisEventLoop.enqueue(appEventLoop, pipeline -> pipeline.zrangeWithScores(key, start, stop));
    }

    @Override
    public RedisFuture<Set<Tuple>> zrangeByScoreWithScores(String key, double min, double max) {
        return redisEventLoop.enqueue(appEventLoop, pipeline -> pipeline.zrangeByScoreWithScores(key, min, max));
    }

    @Override
    public RedisFuture<Set<Tuple>> zrangeByScoreWithScores(String key, double min, double max, int offset, int count) {
        return redisEventLoop.enqueue(appEventLoop, pipeline -> pipeline.zrangeByScoreWithScores(key, min, max, offset, count));
    }

    @Override
    public RedisFuture<Set<Tuple>> zrevrangeByScoreWithScores(String key, double max, double min) {
        return redisEventLoop.enqueue(appEventLoop, pipeline -> pipeline.zrevrangeByScoreWithScores(key, max, min));
    }

    @Override
    public RedisFuture<Set<Tuple>> zrevrangeByScoreWithScores(String key, double max, double min, int offset, int count) {
        return redisEventLoop.enqueue(appEventLoop, pipeline -> pipeline.zrevrangeByScoreWithScores(key, max, min, offset, count));
    }

    @Override
    public RedisFuture<Set<Tuple>> zrevrangeWithScores(String key, long start, long stop) {
        return redisEventLoop.enqueue(appEventLoop, pipeline -> pipeline.zrevrangeWithScores(key, start, stop));
    }

    @Override
    public RedisFuture<Long> zrem(String key, String... members) {
        return redisEventLoop.enqueue(appEventLoop, pipeline -> pipeline.zrem(key, members));
    }

    @Override
    public RedisFuture<Long> zremrangeByRank(String key, long start, long stop) {
        return redisEventLoop.enqueue(appEventLoop, pipeline -> pipeline.zremrangeByRank(key, start, stop));
    }

    @Override
    public RedisFuture<Long> zremrangeByScore(String key, double min, double max) {
        return redisEventLoop.enqueue(appEventLoop, pipeline -> pipeline.zremrangeByScore(key, min, max));
    }

}
