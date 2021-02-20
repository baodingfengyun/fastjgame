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

package com.wjybxx.fastjgame.util.trigger;

import com.wjybxx.fastjgame.util.time.TimeProvider;
import com.wjybxx.fastjgame.util.timer.TimerSystem;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * 触发器系统。
 * 有点像{@link TimerSystem}，但又有区别。
 *
 * @author wjybxx
 * date - 2020/12/7
 * github - https://github.com/hl845740757
 */
public class TriggerSystem {

    private static final Logger logger = LoggerFactory.getLogger(TriggerSystem.class);

    private int lastId;
    private final TimeProvider timeProvider;
    private final Int2ObjectMap<Trigger> triggerMap;

    public TriggerSystem(TimeProvider timeProvider) {
        this(timeProvider, 10);
    }

    public TriggerSystem(TimeProvider timeProvider, int expectedSize) {
        this.timeProvider = timeProvider;
        this.triggerMap = new Int2ObjectOpenHashMap<>(expectedSize);
    }

    /**
     * 创建一个触发器
     * 注意：新增的trigger可能立即update
     */
    public Trigger createTrigger(@Nonnull TriggerHandler handler) {
        Objects.requireNonNull(handler);
        final Trigger trigger = new Trigger(++lastId, timeProvider.curTimeMillis(), handler);
        triggerMap.put(trigger.getTriggerId(), trigger);
        return trigger;
    }

    /**
     * 移除一个触发器
     *
     * @param triggerId 触发器的id
     * @return 被删除的触发器
     */
    public Trigger removeTrigger(int triggerId) {
        final Trigger trigger = triggerMap.get(triggerId);
        if (trigger != null) {
            // 仅仅打上标记，下次删除，由于玩家无法直接创建Trigger，因此一定不会出现相同id的trigger
            trigger.setRemoved(true);
        }
        return trigger;
    }

    /**
     * 清理所有的触发器
     */
    public void clear() {
        triggerMap.clear();
    }

    /**
     * 更新所有的触发器的状态
     */
    public void tickTriggers(long curTimeMillis) {
        if (triggerMap.isEmpty()) {
            return;
        }

        for (ObjectIterator<Trigger> iterator = triggerMap.values().iterator(); iterator.hasNext(); ) {
            Trigger trigger = iterator.next();
            if (trigger.isRemoved()) {
                // 延迟删除，自动清理handler - 避免不经意间的内存泄漏（忘记释放trigger的引用导致handler无法释放）
                trigger.setHandler(null);
                iterator.remove();
                continue;
            }

            try {
                // 先执行更新
                trigger.getHandler().update(trigger, curTimeMillis);
                // 如果尚未删除，且处于激活状态，则尝试触发
                if (!trigger.isRemoved() && trigger.isActive()) {
                    trigger.getHandler().tryTrigger(trigger, curTimeMillis);
                }
            } catch (Exception e) {
                logger.warn("trigger tick caught exception, triggerName {}", trigger.getClass().getName(), e);
            }

            if (trigger.isRemoved()) {
                trigger.setHandler(null);
                iterator.remove();
            }
        }
    }

}