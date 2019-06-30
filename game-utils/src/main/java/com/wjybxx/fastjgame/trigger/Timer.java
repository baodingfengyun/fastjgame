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

package com.wjybxx.fastjgame.trigger;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * 定时器，目前一个timer不允许添加到多个{@link TriggerSystem}
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/4/27 15:01
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public class Timer {
    /**
     * 上次执行的时间戳
     */
    private long lastExecuteMillTime;

    /**
     * 执行间隔(毫秒)
     */
    private long interval;

    /**
     * 剩余执行次数
     */
    private int executeNum;
    /**
     * 执行回调(策略模式的好处是，同一种类型的timer可以有多种类型的回调)
     * 当然也可以不回调，自身完成某个任务
     */
    private final TimerCallBack callBack;

    /**
     * 该timer当前的所有者
     */
    private TriggerSystem owner;

    /**
     * 创建一个定时器实例
     * @param interval 执行间隔(毫秒)
     * @param executeNum 执行次数
     * @param callBack 回调函数
     */
    public Timer(long interval, int executeNum, TimerCallBack callBack) {
        this.interval = interval;
        this.executeNum = executeNum;
        this.callBack = callBack;
    }

    /**
     * 获取timer当前的执行间隔
     * @return interval
     */
    public long getInterval() {
        return interval;
    }

    /**
     * 更新timer的执行间隔
     * @param interval 执行间隔
     */
    public void setInterval(long interval) {
        if (this.interval == interval){
            return;
        }
        this.interval = interval;
        // 执行间隔发生改变，需要通知它的主人进行调整
        if (null != owner){
            owner.priorityChanged(this);
        }
    }

    /**
     * 关闭timer
     */
    public void closeTimer(){
        this.executeNum =0;
    }

    public int getExecuteNum() {
        return executeNum;
    }

    public void setExecuteNum(int executeNum) {
        this.executeNum = executeNum;
    }

    public TimerCallBack getCallBack() {
        return callBack;
    }

    /**
     * 创建一个无限执行的timer
     * @see #Timer(long, int, TimerCallBack)
     * @return executeNum == Integer.MAX_VALUE
     */
    public static Timer newInfiniteTimer(long delayTime,TimerCallBack callBack){
        return new Timer(delayTime,Integer.MAX_VALUE,callBack);
    }

    // ------------------------------------- internal ---------------------------------

    TriggerSystem getOwner() {
        return owner;
    }

    void setOwner(TriggerSystem owner) {
        this.owner = owner;
    }

    /**
     * 获取下次执行的时间戳
     * @return
     */
    long getNextExecuteMillTime(){
        return lastExecuteMillTime + interval;
    }


    long getLastExecuteMillTime() {
        return lastExecuteMillTime;
    }

    void setLastExecuteMillTime(long lastExecuteMillTime) {
        this.lastExecuteMillTime = lastExecuteMillTime;
    }

}
