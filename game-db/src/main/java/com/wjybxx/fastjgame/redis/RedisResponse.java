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

import redis.clients.jedis.exceptions.JedisDataException;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Redis异步操作获取结果的句柄。
 * 注意：它不是{@link java.util.concurrent.Future}，也并不是一个线程安全的对象，
 * 只是用于封装原生的jedis异步返回对象{@link redis.clients.jedis.Response}。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/12/12
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public interface RedisResponse<T> {

    /**
     * 获取最终结果
     *
     * @throws JedisDataException 出现任何异常，都将封装为该异常抛出
     */
    T get();

    /**
     * 查询结果是否已完成
     */
    boolean isDone();

    /**
     * 查询是否已成功完成
     */
    boolean isSuccess();

    /**
     * 获取造成失败的原因
     */
    Throwable cause();

    /**
     * 添加一个回调，当操作完成时，{@link RedisCallback#onComplete(RedisResponse)}将会被调用。
     * 如果操作已完成，该回调将立即执行。
     *
     * @return this
     */
    RedisResponse<T> addCallback(@Nonnull RedisCallback<T> callback);

}
