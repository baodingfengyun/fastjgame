/*
 * Copyright 2019 wjybxx
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wjybxx.fastjgame.utils.time;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * {@link TimeProvider}的实现，提供了切换时间策略方法。
 * 非线程安全。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/4/27 22:06
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public class SimpleTimeProvider implements CachedTimeProvider {

    /**
     * 获取时间策略
     */
    private CachedTimeProvider timeProvider;

    public SimpleTimeProvider() {
        this(TimeProviders.realtimeProvider());
    }

    public SimpleTimeProvider(CachedTimeProvider timeProvider) {
        this.timeProvider = timeProvider;
    }

    /**
     * 获取系统毫秒时间戳
     *
     * @return 毫秒
     */
    @Override
    public long curTimeMillis() {
        return timeProvider.curTimeMillis();
    }

    /**
     * 获取系统秒数时间戳
     *
     * @return 秒
     */
    @Override
    public int curTimeSeconds() {
        return timeProvider.curTimeSeconds();
    }

    /**
     * 尝试更新系统时间
     *
     * @param curTimeMillis 指定的系统毫秒时间
     * @return 更新成功则返回true
     */
    @Override
    public boolean update(long curTimeMillis) {
        return timeProvider.update(curTimeMillis);
    }

    /**
     * 切换到缓存策略
     */
    public void changeToCacheStrategy() {
        this.timeProvider = TimeProviders.newCachedTimeProvider(System.currentTimeMillis());
    }

    /**
     * 切换到实时策略
     */
    public void changeToRealTimeStrategy() {
        this.timeProvider = TimeProviders.realtimeProvider();
    }

    @Override
    public String toString() {
        return "SimpleTimeProvider{" +
                "timeProvider=" + timeProvider +
                '}';
    }
}
