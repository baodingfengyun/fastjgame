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
import javax.annotation.Nullable;

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

    /**
     * 如果一个操作成功时没有结果使用该对象代替。{@link #onComplete(Object) null}
     */
    private static final Object SUCCESS = new Object();

    /**
     * redis命令执行结果 - 我们使用 null 表示命令尚未执行完成状态。
     */
    private Object result;
    /**
     * 用户的回调逻辑
     */
    private RedisCallback<T> callback;

    DefaultRedisResponse() {

    }

    @Override
    public final T get() {
        if (!isDone()) {
            throw new JedisDataException("uncompleted");
        }

        if (result instanceof JedisDataException) {
            throw (JedisDataException) result;
        }

        return getNullableResponse();
    }

    @SuppressWarnings("unchecked")
    private T getNullableResponse() {
        return result == SUCCESS ? null : (T) result;
    }

    @Override
    public final T getNow() {
        if (isSuccess()) {
            return getNullableResponse();
        } else {
            return null;
        }
    }

    @Override
    public final boolean isDone() {
        return null != result;
    }

    @Override
    public final boolean isSuccess() {
        return isDone() && !(result instanceof JedisDataException);
    }

    @Override
    public final JedisDataException cause() {
        return result instanceof JedisDataException ? (JedisDataException) result : null;
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
    final void onComplete(@Nullable final Object data) {
        if (isDone()) {
            throw new IllegalStateException("completed already");
        }

        if (data == null) {
            result = SUCCESS;
        } else {
            result = data;
        }

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
