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

package com.wjybxx.fastjgame.db.redis;

import com.wjybxx.fastjgame.utils.async.MethodSpec;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;

/**
 * redis命令
 * (目前是基于{@link Pipeline}的命令)
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/12/12
 * github - https://github.com/hl845740757
 */
@FunctionalInterface
public interface RedisCommand<V> extends MethodSpec<V> {

    /**
     * 执行相应的管道命令
     *
     * @apiNote 该命令执行在redis线程
     */
    Response<V> execute(Pipeline pipeline);

}
