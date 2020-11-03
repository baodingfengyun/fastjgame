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

package com.wjybxx.fastjgame.util.misc;

import com.wjybxx.fastjgame.util.exception.InfiniteLoopException;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * 无限循环预防。
 * <p>
 * Q: 什么时候应该使用？<br>
 * A: 但凡可能大量循环的地方都应该使用，尤其是带有随机性质的功能（方法），比如：随机10个不重复的玩家名字。<br>
 * </p>
 *
 * @author wjybxx
 * date - 2020/11/3
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public final class InfiniteLoopDefender {

    private static final int DEFAULT_LOOP_LIMIT = 1000;

    private final int loopLimit;
    private int curLoop = 0;

    public InfiniteLoopDefender() {
        this.loopLimit = DEFAULT_LOOP_LIMIT;
    }

    public InfiniteLoopDefender(int loopLimit) {
        this.loopLimit = loopLimit;
    }

    /**
     * 检查是否超出循环限制
     * 注意：需要在每个循环内调用check
     */
    public void check() {
        if (++curLoop > loopLimit) {
            final String msg = String.format("loopLimit %d, curLoop %d", loopLimit, curLoop);
            throw new InfiniteLoopException(msg);
        }
    }

    /**
     * @return 允许的最大循环次数
     */
    public int getLoopLimit() {
        return loopLimit;
    }

    /**
     * @return 获取当前循环次数
     */
    public int getCurLoop() {
        return curLoop;
    }

    /**
     * @return 获取剩余可循环次数
     */
    public int availableLoop() {
        return loopLimit - curLoop;
    }

    /**
     * 重置循环计数
     *
     * @deprecated 该对象是个很轻量级的对象，不建议重复使用，避免忘记重置导致的bug。
     * 如果觉得很有必要重用，请保证你进行了正确的重置，再使用{@link SuppressWarnings}压制警告。
     */
    @Deprecated
    public void reset() {
        curLoop = 0;
    }

}
