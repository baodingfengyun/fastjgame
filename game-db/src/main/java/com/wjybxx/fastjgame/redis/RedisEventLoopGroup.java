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
import com.wjybxx.fastjgame.concurrent.EventLoopChooserFactory;
import com.wjybxx.fastjgame.concurrent.MultiThreadEventLoopGroup;
import com.wjybxx.fastjgame.concurrent.RejectedExecutionHandler;
import redis.clients.jedis.JedisPoolAbstract;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.ThreadFactory;

/**
 * redis线程池
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/12/12
 * github - https://github.com/hl845740757
 */
public class RedisEventLoopGroup extends MultiThreadEventLoopGroup {

    public RedisEventLoopGroup(int nThreads,
                               @Nonnull ThreadFactory threadFactory,
                               @Nonnull RejectedExecutionHandler rejectedExecutionHandler,
                               @Nonnull JedisPoolAbstract jedisPool) {
        super(nThreads, threadFactory, rejectedExecutionHandler, jedisPool);
    }

    public RedisEventLoopGroup(int nThreads,
                               @Nonnull ThreadFactory threadFactory,
                               @Nonnull RejectedExecutionHandler rejectedExecutionHandler,
                               @Nullable EventLoopChooserFactory chooserFactory,
                               @Nonnull JedisPoolAbstract jedisPool) {
        super(nThreads, threadFactory, rejectedExecutionHandler, chooserFactory, jedisPool);
    }

    @Nonnull
    @Override
    protected EventLoop newChild(int childIndex, ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler, Object context) {
        return new RedisEventLoop(this, threadFactory, rejectedExecutionHandler, (JedisPoolAbstract) context);
    }

    @Override
    protected void clean() {
        ((JedisPoolAbstract) context).close();
    }
}
