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

package com.wjybxx.fastjgame.mgr;

import com.google.inject.Inject;
import com.wjybxx.fastjgame.redis.RedisFuture;
import com.wjybxx.fastjgame.redis.RedisPipeline;
import redis.clients.jedis.ListPosition;
import redis.clients.jedis.Tuple;
import redis.clients.jedis.params.ZAddParams;
import redis.clients.jedis.params.ZIncrByParams;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * redis管理器
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/12/12
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public class RedisMgr {

    private final RedisEventLoopMgr redisEventLoopMgr;
    private final GameEventLoopMgr gameEventLoopMgr;
    private RedisPipeline redisPipeline;

    @Inject
    public RedisMgr(RedisEventLoopMgr redisEventLoopMgr, GameEventLoopMgr gameEventLoopMgr) {
        // 在构造的时候无法确保gameEventLoop存在
        this.redisEventLoopMgr = redisEventLoopMgr;
        this.gameEventLoopMgr = gameEventLoopMgr;
    }

    /**
     * 在使用其它方法之前，必须先构建管道
     */
    public void pipelined() {
        if (redisPipeline != null) {
            throw new IllegalStateException();
        }
        redisPipeline = redisEventLoopMgr.newPipeline(gameEventLoopMgr.getEventLoop());
    }

    public RedisFuture<Long> incr(String key) {
        return redisPipeline.incr(key);
    }

    public RedisFuture<Long> incrBy(String key, long increment) {
        return redisPipeline.incrBy(key, increment);
    }

    public RedisFuture<String> lindex(String key, long index) {
        return redisPipeline.lindex(key, index);
    }

    public RedisFuture<Long> linsert(String key, ListPosition where, String pivot, String value) {
        return redisPipeline.linsert(key, where, pivot, value);
    }

    public RedisFuture<String> lpop(String key) {
        return redisPipeline.lpop(key);
    }

    public RedisFuture<Long> lpush(String key, String... string) {
        return redisPipeline.lpush(key, string);
    }

    public RedisFuture<Long> lpushx(String key, String... string) {
        return redisPipeline.lpushx(key, string);
    }

    public RedisFuture<String> rpop(String key) {
        return redisPipeline.rpop(key);
    }

    public RedisFuture<Long> rpush(String key, String... string) {
        return redisPipeline.rpush(key, string);
    }

    public RedisFuture<Long> rpushx(String key, String... string) {
        return redisPipeline.rpushx(key, string);
    }

    public RedisFuture<List<String>> lrange(String key, long start, long stop) {
        return redisPipeline.lrange(key, start, stop);
    }

    public RedisFuture<Long> lrem(String key, long count, String value) {
        return redisPipeline.lrem(key, count, value);
    }

    public RedisFuture<String> lset(String key, long index, String value) {
        return redisPipeline.lset(key, index, value);
    }

    public RedisFuture<String> ltrim(String key, long start, long stop) {
        return redisPipeline.ltrim(key, start, stop);
    }

    public RedisFuture<Long> llen(String key) {
        return redisPipeline.llen(key);
    }

    public RedisFuture<Boolean> hexists(String key, String field) {
        return redisPipeline.hexists(key, field);
    }

    public RedisFuture<String> hget(String key, String field) {
        return redisPipeline.hget(key, field);
    }

    public RedisFuture<Long> hdel(String key, String... field) {
        return redisPipeline.hdel(key, field);
    }

    public RedisFuture<Long> hincrBy(String key, String field, long value) {
        return redisPipeline.hincrBy(key, field, value);
    }

    public RedisFuture<Long> hset(String key, String field, String value) {
        return redisPipeline.hset(key, field, value);
    }

    public RedisFuture<Long> hsetnx(String key, String field, String value) {
        return redisPipeline.hsetnx(key, field, value);
    }

    public RedisFuture<List<String>> hmget(String key, String... fields) {
        return redisPipeline.hmget(key, fields);
    }

    public RedisFuture<String> hmset(String key, Map<String, String> hash) {
        return redisPipeline.hmset(key, hash);
    }

    public RedisFuture<List<String>> hvals(String key) {
        return redisPipeline.hvals(key);
    }

    public RedisFuture<Long> hlen(String key) {
        return redisPipeline.hlen(key);
    }

    public RedisFuture<Set<String>> hkeys(String key) {
        return redisPipeline.hkeys(key);
    }

    public RedisFuture<Map<String, String>> hgetAll(String key) {
        return redisPipeline.hgetAll(key);
    }

    public RedisFuture<Long> zadd(String key, double score, String member) {
        return redisPipeline.zadd(key, score, member);
    }

    public RedisFuture<Long> zadd(String key, double score, String member, ZAddParams params) {
        return redisPipeline.zadd(key, score, member, params);
    }

    public RedisFuture<Long> zadd(String key, Map<String, Double> scoreMembers) {
        return redisPipeline.zadd(key, scoreMembers);
    }

    public RedisFuture<Long> zadd(String key, Map<String, Double> scoreMembers, ZAddParams params) {
        return redisPipeline.zadd(key, scoreMembers, params);
    }

    public RedisFuture<Long> zcard(String key) {
        return redisPipeline.zcard(key);
    }

    public RedisFuture<Long> zcount(String key, double min, double max) {
        return redisPipeline.zcount(key, min, max);
    }

    public RedisFuture<Double> zincrby(String key, double increment, String member) {
        return redisPipeline.zincrby(key, increment, member);
    }

    public RedisFuture<Double> zincrby(String key, double increment, String member, ZIncrByParams params) {
        return redisPipeline.zincrby(key, increment, member, params);
    }

    public RedisFuture<Long> zrank(String key, String member) {
        return redisPipeline.zrank(key, member);
    }

    public RedisFuture<Long> zrevrank(String key, String member) {
        return redisPipeline.zrevrank(key, member);
    }

    public RedisFuture<Double> zscore(String key, String member) {
        return redisPipeline.zscore(key, member);
    }

    public RedisFuture<Set<Tuple>> zrangeWithScores(String key, long start, long stop) {
        return redisPipeline.zrangeWithScores(key, start, stop);
    }

    public RedisFuture<Set<Tuple>> zrangeByScoreWithScores(String key, double min, double max) {
        return redisPipeline.zrangeByScoreWithScores(key, min, max);
    }

    public RedisFuture<Set<Tuple>> zrangeByScoreWithScores(String key, double min, double max, int offset, int count) {
        return redisPipeline.zrangeByScoreWithScores(key, min, max, offset, count);
    }

    public RedisFuture<Set<Tuple>> zrevrangeByScoreWithScores(String key, double max, double min) {
        return redisPipeline.zrevrangeByScoreWithScores(key, max, min);
    }

    public RedisFuture<Set<Tuple>> zrevrangeByScoreWithScores(String key, double max, double min, int offset, int count) {
        return redisPipeline.zrevrangeByScoreWithScores(key, max, min, offset, count);
    }

    public RedisFuture<Set<Tuple>> zrevrangeWithScores(String key, long start, long stop) {
        return redisPipeline.zrevrangeWithScores(key, start, stop);
    }

    public RedisFuture<Long> zrem(String key, String... members) {
        return redisPipeline.zrem(key, members);
    }

    public RedisFuture<Long> zremrangeByRank(String key, long start, long stop) {
        return redisPipeline.zremrangeByRank(key, start, stop);
    }

    public RedisFuture<Long> zremrangeByScore(String key, double min, double max) {
        return redisPipeline.zremrangeByScore(key, min, max);
    }
}
