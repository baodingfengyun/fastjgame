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

package com.wjybxx.fastjgame.utils.timer;

/**
 * 定时器任务。
 * Q: 为什么{@link #run(TimerHandle)}不再是泛型参数？
 * A: 解除耦合，{@link TimerHandle}属于控制单元，而{@link TimerTask}仅仅是执行单元。执行单元不应该过多的了解控制单元的属性。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/7
 * github - https://github.com/hl845740757
 */
@FunctionalInterface
public interface TimerTask {

    /**
     * 执行需要的任务
     *
     * @param handle 该任务绑定的句柄，可以获取一些附加属性。
     * @apiNote 如果运行时抛出异常，则会取消执行
     */
    void run(TimerHandle handle) throws Exception;

}
