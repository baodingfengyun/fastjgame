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

package com.wjybxx.fastjgame.timer;

import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;

/**
 * 系统时间帮助类，非线程安全。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/4/27 22:06
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public class SystemTimeHelper implements SystemTimeProvider{

    /**
     * 获取时间策略
     */
    private SystemTimeStrategy strategy = RealTimeStrategy.INSTANCE;

    public SystemTimeHelper() {

    }

    /**
     * 获取系统毫秒时间戳
     * @return 毫秒
     */
    @Override
    public long getSystemMillTime() {
        return strategy.getSystemMillTime();
    }

    /**
     * 获取系统秒数时间戳
     * @return 秒
     */
    @Override
    public int getSystemSecTime() {
        return strategy.getSystemSecTime();
    }

    /**
     * 尝试更新系统时间
     * @param systemTimeMillis 指定的系统毫秒时间
     * @return 更新成功则返回true
     */
    public boolean update(long systemTimeMillis){
        return strategy.update(systemTimeMillis);
    }

    /**
     * 切换到缓存策略
     */
    public void changeToCacheStrategy(){
        this.strategy = new CacheTimeStrategy();
    }

    /**
     * 切换到实时策略
     */
    public void changeToRealTimeStrategy(){
        this.strategy = RealTimeStrategy.INSTANCE;
    }

    @Override
    public String toString() {
        return "SystemTimeHelper{" +
                "strategy=" + strategy +
                '}';
    }


    public interface SystemTimeStrategy extends SystemTimeProvider {

        @Override
        long getSystemMillTime();

        @Override
        int getSystemSecTime();

        /**
         * 尝试更新系统时间
         * @param systemTimeMillis 指定的系统毫秒时间
         * @return 更新成功则返回true
         */
        boolean update(long systemTimeMillis);
    }

    /**
     * 缓存时间提供者，有特定的更新方法.
     * 目的为了减少频繁地调用{@link System#currentTimeMillis()}
     */
    @NotThreadSafe
    public static class CacheTimeStrategy implements SystemTimeStrategy{
        /**
         * 当前帧毫秒时间(非实时时间)
         */
        private long systemMillTime;
        /**
         * 当前帧秒时间(非实时时间)
         */
        private int systemSecTime;

        CacheTimeStrategy() {
            update(System.currentTimeMillis());
        }

        public boolean update(long systemTimeMillis) {
            this.systemMillTime = systemTimeMillis;
            this.systemSecTime = (int) (systemTimeMillis /1000);
            return true;
        }

        @Override
        public long getSystemMillTime() {
            return systemMillTime;
        }

        @Override
        public int getSystemSecTime() {
            return systemSecTime;
        }

        @Override
        public String toString() {
            return "CacheTimeStrategy{" +
                    "systemMillTime=" + systemMillTime +
                    ", systemSecTime=" + systemSecTime +
                    '}';
        }
    }

    /**
     * 实时系统时间提供者
     */
    @ThreadSafe
    public static class RealTimeStrategy implements SystemTimeStrategy{

        /** 实时策略，它本身是线程安全的*/
        public static final RealTimeStrategy INSTANCE = new RealTimeStrategy();

        private RealTimeStrategy() {

        }

        @Override
        public long getSystemMillTime() {
            return System.currentTimeMillis();
        }

        @Override
        public int getSystemSecTime() {
            return (int) (System.currentTimeMillis()/1000);
        }

        @Override
        public boolean update(long systemTimeMillis) {
            return false;
        }
    }

}
