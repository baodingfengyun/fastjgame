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

package com.wjybxx.fastjgame.utils;

import java.util.Set;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/12
 * github - https://github.com/hl845740757
 */
public class ThreadUtils {

    /**
     * J9中新的的{@link StackWalker}，拥有更好的性能，因为它可以只创建需要的栈帧，而不是像异常一样总是获得所有栈帧的信息。
     */
    private static final StackWalker STACK_WALKER = StackWalker.getInstance(Set.of(StackWalker.Option.SHOW_HIDDEN_FRAMES));

    /**
     * 恢复中断。
     * 如果是中断异常，则恢复线程中断状态。
     *
     * @param t 异常
     */
    public static void recoveryInterrupted(Throwable t) {
        if (t instanceof InterruptedException) {
            try {
                Thread.currentThread().interrupt();
            } catch (SecurityException ignore) {
            }
        }
    }

    /**
     * 恢复中断
     *
     * @param interrupted 是否出现了中断
     */
    public static void recoveryInterrupted(boolean interrupted) {
        if (interrupted) {
            try {
                Thread.currentThread().interrupt();
            } catch (SecurityException ignore) {
            }
        }
    }

    /**
     * 检查线程中断状态。
     *
     * @throws InterruptedException 如果线程被中断，则抛出中断异常
     */
    public static void checkInterrupted() throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
    }

    /**
     * 安静地睡眠一会儿
     *
     * @param sleepMillis 要睡眠的时间(毫秒)
     */
    public static void sleepQuietly(int sleepMillis) {
        try {
            Thread.sleep(sleepMillis);
        } catch (InterruptedException ignore) {

        }
    }

    /**
     * 获取调用者信息
     *
     * @param deep 当前方法的深度
     * @return 调用者信息
     */
    public static String getCallerInfo(int deep) {
        // +1 对应当前方法的栈帧
        // Q: 为什么使用limit而不是skip?
        // A: 如果方法被内联，则skip跳过的栈帧可能不正确，因此使用limit，并选择最后一个栈帧
        return STACK_WALKER.walk(stackFrameStream -> stackFrameStream.limit(deep + 1)
                .reduce((stackFrame, stackFrame2) -> stackFrame2)
                .map(Object::toString))
                .orElseThrow();
    }
}
