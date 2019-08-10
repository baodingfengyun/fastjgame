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

package com.wjybxx.fastjgame.gameobject;

import com.wjybxx.fastjgame.misc.ViewGrid;
import com.wjybxx.fastjgame.scene.Scene;
import com.wjybxx.fastjgame.scene.gameobjectdata.GameObjectData;
import com.wjybxx.fastjgame.scene.gameobjectdata.GameObjectType;
import com.wjybxx.fastjgame.shape.Point2D;
import com.wjybxx.fastjgame.trigger.*;

import javax.annotation.Nonnull;

/**
 * 场景对象顶层类。
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/31 22:48
 * github - https://github.com/hl845740757
 */
public abstract class GameObject<T extends GameObjectData> {

    /** 对象当前所在的场景 */
    protected Scene scene;
    /** 对象绑定的定时器 */
    private final TimerSystem timerSystem;
    /**
     * 游戏对象的坐标
     * 先写波2D的，练手AOI
     */
    private final Point2D position = Point2D.newPoint2D();
    /**
     * 对象当前所在的视野格子;
     * 缓存的视野格子，用于降低视野更新频率；
     * 游戏对象进入视野时需要立即初始化，离开后需要删除；
     */
    private ViewGrid viewGrid;

    public GameObject(Scene scene, SystemTimeProvider timeProvider) {
        this.scene = scene;
        this.timerSystem = new DefaultTimerSystem(timeProvider);
    }

    public Point2D getPosition() {
        return position;
    }

    @Nonnull
    public ViewGrid getViewGrid() {
        return viewGrid;
    }

    public void setViewGrid(ViewGrid viewGrid) {
        this.viewGrid = viewGrid;
    }

    public final long getGuid() {
        return getData().getGuid();
    }

    /**
     * 返回该场景对象对应的枚举类型
     * @return {@link GameObjectType}
     */
    @Nonnull
    public final GameObjectType getObjectType() {
        return getData().getObjectType();
    }
    /**
     * 获取该场景对象的非场景数据
     * @return data
     */
    @Nonnull
    public abstract T getData();

    public void tick() {
        timerSystem.tick();
    }

    @Nonnull
    public TimeoutHandle newTimeout(long timeout, @Nonnull TimerTask<TimeoutHandle> task) {
        return timerSystem.newTimeout(timeout, task);
    }

    public TimeoutHandle nextTick(@Nonnull TimerTask<TimeoutHandle> task) {
        return timerSystem.nextTick(task);
    }

    @Nonnull
    public FixedDelayHandle newFixedDelay(long initialDelay, long delay, @Nonnull TimerTask<FixedDelayHandle> task) {
        return timerSystem.newFixedDelay(initialDelay, delay, task);
    }

    @Nonnull
    public FixedDelayHandle newFixedDelay(long delay, @Nonnull TimerTask<FixedDelayHandle> task) {
        return timerSystem.newFixedDelay(delay, task);
    }

    @Nonnull
    public FixedRateHandle newFixRate(long initialDelay, long period, @Nonnull TimerTask<FixedRateHandle> task) {
        return timerSystem.newFixRate(initialDelay, period, task);
    }

    @Nonnull
    public FixedRateHandle newFixRate(long period, @Nonnull TimerTask<FixedRateHandle> task) {
        return timerSystem.newFixRate(period, task);
    }
}
