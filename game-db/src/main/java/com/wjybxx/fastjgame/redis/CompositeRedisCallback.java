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

import java.util.ArrayList;
import java.util.List;

/**
 * 为多个回调提供单一视图
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/12/12
 * github - https://github.com/hl845740757
 */
public class CompositeRedisCallback<T> implements RedisCallback<T> {

    private static final Logger logger = LoggerFactory.getLogger(CompositeRedisCallback.class);

    private final List<RedisCallback<T>> children = new ArrayList<>(2);

    public CompositeRedisCallback() {

    }

    public CompositeRedisCallback(RedisCallback<T> first, RedisCallback<T> second) {
        children.add(first);
        children.add(second);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onComplete(RedisResponse<T> response) {
        for (RedisCallback redisCallback : children) {
            try {
                redisCallback.onComplete(response);
            } catch (Throwable e) {
                logger.warn("Child onComplete caught exception!", e);
            }
        }
    }

    /**
     * 添加一个回调。
     * 该方法需要保证所有添加的回调都能执行。
     */
    public CompositeRedisCallback<T> addChild(RedisCallback<T> callback) {
        children.add(callback);
        return this;
    }

    /**
     * 删除第一个匹配的回调
     */
    public boolean remove(RedisCallback callback) {
        return children.remove(callback);
    }
}
