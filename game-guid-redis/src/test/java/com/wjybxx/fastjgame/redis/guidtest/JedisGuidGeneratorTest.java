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

package com.wjybxx.fastjgame.redis.guidtest;

import com.wjybxx.fastjgame.redis.guid.RedisGuidGenerator;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolAbstract;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/13
 * github - https://github.com/hl845740757
 */
public class JedisGuidGeneratorTest {

    public static void main(String[] args) {
        final JedisPoolAbstract jedisPool = newJedisPool();
        try {
            doTest(jedisPool, "player");
            System.out.println("-------------------------------------------");
            doTest(jedisPool, "monster");
        } finally {
            jedisPool.close();
        }
    }

    private static JedisPoolAbstract newJedisPool() {
        return new JedisPool("localhost", 6379);
    }

    private static void doTest(JedisPoolAbstract jedisPool, String name) {
        final int cacheSize = 100;
        final RedisGuidGenerator guidGenerator = new RedisGuidGenerator(jedisPool, name, cacheSize);
        try {
            for (int index = 0; index < cacheSize * 3; index++) {
                System.out.println("name: " + name + ", guid: " + guidGenerator.next());
            }
        } finally {
            guidGenerator.close();
        }

    }
}
