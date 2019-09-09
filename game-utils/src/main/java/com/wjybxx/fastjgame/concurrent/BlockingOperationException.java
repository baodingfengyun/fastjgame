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

package com.wjybxx.fastjgame.concurrent;

/**
 * 当用户在事件循环线程中执行了一个阻塞操作时将会抛出一个{@link BlockingOperationException}异常。
 * 在事件循环线程中执行一个阻塞操作，该阻塞操作可能导致线程进入死锁状态，因此在检测到可能死锁时，抛出该异常。
 * <p>
 * 死锁分析：
 * EventLoop是单线程的，线程一次只能执行一个任务，如果在执行任务的时候等待该线程上的另一个任务完成，将死锁。
 * <p>
 * copy from netty，在这里并不希望依赖netty。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/24
 * github - https://github.com/hl845740757
 */
public class BlockingOperationException extends IllegalStateException {

    private static final long serialVersionUID = 2462223247762460301L;

    public BlockingOperationException() {
    }

    public BlockingOperationException(String s) {
        super(s);
    }

    public BlockingOperationException(String message, Throwable cause) {
        super(message, cause);
    }

    public BlockingOperationException(Throwable cause) {
        super(cause);
    }
}
