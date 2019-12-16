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

import redis.clients.jedis.exceptions.JedisConnectionException;

/**
 * redis连接异常。
 * 基于性能方面的考虑，该异常并不填充堆栈 - 填充也没有太大意义。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/12/16
 * github - https://github.com/hl845740757
 */
public class RedisConnectionException extends JedisConnectionException {

    static final RedisConnectionException INSTANCE = new RedisConnectionException();

    private RedisConnectionException() {
        // singleton
        super("Could not get a resource from the pool");
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
