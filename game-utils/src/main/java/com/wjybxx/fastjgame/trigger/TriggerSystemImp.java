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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * 触发器系统，使用优先级队列存储timer，以获得更好的性能。
 * @author wjybxx
 * @version 1.0
 * date - 2019/4/27 15:06
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public class TriggerSystemImp implements TriggerSystem {

    private static final Logger logger = LoggerFactory.getLogger(TriggerSystemImp.class);

    private static final Comparator<Timer> timerComparator = Comparator.comparingLong(Timer::getNextExecuteMillTime);

    private final PriorityQueue<Timer> timerQueue = new PriorityQueue<>(timerComparator);

    public TriggerSystemImp() {

    }

    /**
     * 移除指定定时器
     * @param timer 要删除的定时器
     * @return 如果删除成功(timer存在于触发器系统中)则返回true，否则返回false；
     */
    public final boolean removeTimer(Timer timer){
        return timerQueue.remove(timer);
    }

    @Override
    public final void addTimer(Timer timer,long curMillTime) {
        timer.setLastExecuteMillTime(curMillTime);
        timer.setOwner(this);
        timerQueue.offer(timer);
    }


    /**
     * 检查所有的定时器，执行哪些可以执行的定时器。
     * @param curMillTime 当前时间戳
     */
    @Override
    public final void tickTrigger(long curMillTime) {
        while (timerQueue.size()>0){
            //取出优先级最高的timer，若不满足执行时间，则直接返回(peek查询首元素若没有元素则返回null)
            Timer timer=timerQueue.peek();
            if (curMillTime<timer.getNextExecuteMillTime()){
                break;
            }

            //时间到了，先弹出
            timerQueue.poll();//poll若没有元素，则返回null

            //已执行完毕(取消执行)，无需重新压入
            if (timer.getExecuteNum()<=0){
                timer.setOwner(null);
                continue;
            }

            try{
                timer.setExecuteNum(timer.getExecuteNum()-1);
                timer.getCallBack().callBack(timer);
            }catch (Exception e){
                logger.warn("timer callback caught exception",e);
            }

            //已执行完毕，无需重新压入
            if (timer.getExecuteNum()<=0){
                timer.setOwner(null);
                continue;
            }

            //修改上次执行的时间戳，必须重新压入
            timer.setLastExecuteMillTime(curMillTime);
            timerQueue.offer(timer);//若超过队列上限，则返回false
        }
    }

    @Override
    public void priorityChanged(Timer timer) {
        // 重新放入堆结构，这是比较笨的方法
        timerQueue.remove(timer);
        timerQueue.add(timer);
    }
}
