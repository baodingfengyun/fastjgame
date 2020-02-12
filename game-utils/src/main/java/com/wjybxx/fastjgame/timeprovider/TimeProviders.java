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

package com.wjybxx.fastjgame.timeprovider;


import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;

/**
 * 常用系统时间提供器
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/16
 */
public final class TimeProviders {

    private TimeProviders() {

    }

    /**
     * 获取实时时间提供器，它实现了{@link CachedTimeProvider}接口。
     *
     * @return timeProvider - threadSafe
     */
    public static CachedTimeProvider realtimeProvider() {
        return RealTimeProvider.INSTANCE;
    }

    /**
     * 创建一个支持缓存的时间提供器，你需要调用{@link CachedTimeProvider#update(long)}更新时间值。
     *
     * @param curTimeMillis 初始系统时间
     * @param threadSafe    是否需要线程安全保障
     * @return timeProvider
     */
    public static CachedTimeProvider newCachedTimeProvider(long curTimeMillis, boolean threadSafe) {
        if (threadSafe) {
            return new ThreadSafeCachedTimeProvider(curTimeMillis);
        } else {
            return new UnsharableCachedTimeProvider(curTimeMillis);
        }
    }

    /**
     * 实时系统时间提供者
     */
    @ThreadSafe
    private static class RealTimeProvider implements CachedTimeProvider {

        public static final RealTimeProvider INSTANCE = new RealTimeProvider();

        private RealTimeProvider() {

        }

        @Override
        public long curTimeMillis() {
            return System.currentTimeMillis();
        }

        @Override
        public int curTimeSeconds() {
            return (int) (System.currentTimeMillis() / 1000);
        }

        @Override
        public boolean update(long curTimeMillis) {
            return false;
        }
    }

    @NotThreadSafe
    private static class UnsharableCachedTimeProvider implements CachedTimeProvider {

        private long curTimeMillis;

        private UnsharableCachedTimeProvider(long curTimeMillis) {
            update(curTimeMillis);
        }

        public boolean update(long curTimeMillis) {
            this.curTimeMillis = curTimeMillis;
            return true;
        }

        @Override
        public long curTimeMillis() {
            return curTimeMillis;
        }

        @Override
        public int curTimeSeconds() {
            return (int) (curTimeMillis / 1000);
        }

        @Override
        public String toString() {
            return "UnsharableCachedTimeProvider{" +
                    "curTimeMillis=" + curTimeMillis +
                    '}';
        }
    }

    @ThreadSafe
    private static class ThreadSafeCachedTimeProvider implements CachedTimeProvider {

        /**
         * 缓存一个值而不是多个值，可以实现原子更新。
         * 缓存多个值时，多个值之间具有联系，需要使用对象封装才能原子更新。
         */
        private volatile long curTimeMillis;

        private ThreadSafeCachedTimeProvider(long curTimeMillis) {
            update(curTimeMillis);
        }

        public boolean update(long curTimeMillis) {
            this.curTimeMillis = curTimeMillis;
            return true;
        }

        @Override
        public long curTimeMillis() {
            return curTimeMillis;
        }

        @Override
        public int curTimeSeconds() {
            return (int) (curTimeMillis / 1000);
        }

        @Override
        public String toString() {
            return "ThreadSafeCachedTimeProvider{" +
                    "curTimeMillis=" + curTimeMillis +
                    '}';
        }
    }
}
