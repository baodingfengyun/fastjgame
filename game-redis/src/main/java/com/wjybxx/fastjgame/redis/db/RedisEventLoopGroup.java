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

package com.wjybxx.fastjgame.redis.db;

import com.wjybxx.fastjgame.util.concurrent.AbstractFixedEventLoopGroup;
import com.wjybxx.fastjgame.util.concurrent.EventLoopChooserFactory;
import com.wjybxx.fastjgame.util.concurrent.RejectedExecutionHandler;
import redis.clients.jedis.JedisPoolAbstract;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.ThreadFactory;

/**
 * redis线程池
 * 注意：jedis连接池为外部资源，并不会主动释放，用户如果需要关闭，请监听线程池终止事件，在回调逻辑中关闭连接池。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/12/12
 * github - https://github.com/hl845740757
 */
public class RedisEventLoopGroup extends AbstractFixedEventLoopGroup {

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
    public RedisEventLoop next() {
        return (RedisEventLoop) super.next();
    }

    @Nonnull
    @Override
    public RedisEventLoop select(int key) {
        return (RedisEventLoop) super.select(key);
    }

    @Nonnull
    @Override
    protected RedisEventLoop newChild(int childIndex, ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler, Object context) {
        return new RedisEventLoop(this, threadFactory, rejectedExecutionHandler, (JedisPoolAbstract) context);
    }

}
