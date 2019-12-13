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


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Response;
import redis.clients.jedis.exceptions.JedisDataException;

import javax.annotation.Nonnull;

/**
 * redis异步操作结果的默认实现
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/12/12
 * github - https://github.com/hl845740757
 */
public class DefaultRedisResponse<T> implements RedisResponse<T> {

    private static final Logger logger = LoggerFactory.getLogger(DefaultRedisResponse.class);

    private RedisCallback<T> callback;

    private T response;
    private JedisDataException exception;

    DefaultRedisResponse() {

    }

    @Override
    public final T get() {
        if (exception != null) {
            throw exception;
        }

        if (null != response) {
            return response;
        }

        throw new JedisDataException("uncompleted");
    }

    @Override
    public final T getNow() {
        return response;
    }

    @Override
    public final boolean isDone() {
        return null != response || exception != null;
    }

    @Override
    public final boolean isSuccess() {
        return null != response;
    }

    @Override
    public final JedisDataException cause() {
        return exception;
    }

    @Override
    public final RedisResponse<T> addCallback(@Nonnull RedisCallback<T> newCallback) {
        if (isDone()) {
            notifyListeners(this, newCallback);
            return this;
        }

        if (null == this.callback) {
            this.callback = newCallback;
            return this;
        }

        if (this.callback instanceof CompositeRedisCallback) {
            ((CompositeRedisCallback<T>) this.callback).addChild(newCallback);
        } else {
            this.callback = new CompositeRedisCallback<>(this.callback, newCallback);
        }

        return this;
    }

    /**
     * 当{@link Response}对应的操作已真正完成时，该方法将被调用。
     */
    final void onComplete(final T response, final JedisDataException exception) {
        this.response = response;
        this.exception = exception;
        notifyListeners(this, callback);
    }

    private static <T> void notifyListeners(RedisResponse<T> response, RedisCallback<T> callback) {
        if (null != callback) {
            try {
                callback.onComplete(response);
            } catch (Throwable throwable) {
                logger.warn("callback.onComplete caught exception", throwable);
            }
        }
    }
}
