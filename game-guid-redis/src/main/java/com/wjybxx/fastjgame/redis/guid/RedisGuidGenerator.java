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

package com.wjybxx.fastjgame.redis.guid;

import com.wjybxx.fastjgame.guid.core.GuidGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPoolAbstract;

/**
 * 基于redis实现的guid生成方案
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/13
 * github - https://github.com/hl845740757
 */
public class RedisGuidGenerator implements GuidGenerator {

    private static final Logger logger = LoggerFactory.getLogger(RedisGuidGenerator.class);

    private static final String GUID_HASH_KEY = "_guid";
    private static final int DEFAULT_CACHE_SIZ = 1000_000;

    private final JedisPoolAbstract jedisPool;
    private final String name;
    private final int cacheSize;

    private long curGuid = 0;
    private long curBarrier = 0;


    public RedisGuidGenerator(JedisPoolAbstract jedisPool, String name) {
        this(jedisPool, name, DEFAULT_CACHE_SIZ);
    }

    /**
     * @param name      生成器名字
     * @param cacheSize 每次缓存大小
     */
    public RedisGuidGenerator(JedisPoolAbstract jedisPool, String name, int cacheSize) {
        if (cacheSize <= 0) {
            throw new IllegalArgumentException("cacheSize: " + cacheSize + " (expected: > 0)");
        }
        this.jedisPool = jedisPool;
        this.name = name;
        this.cacheSize = cacheSize;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public long next() {
        checkCache();

        return curGuid++;
    }

    private void checkCache() {
        if (curGuid > 0 && curGuid <= curBarrier) {
            return;
        }
        refreshCache();
    }

    private void refreshCache() {
        try (Jedis jedis = jedisPool.getResource()) {
            curBarrier = jedis.hincrBy(GUID_HASH_KEY, name, cacheSize);
            curGuid = curBarrier - cacheSize + 1;
            logger.info("update guid cache, curGuid={}, curBarrier={}", curGuid, curBarrier);
        }
    }

    @Override
    public void close() {
        // do nothing
    }
}
