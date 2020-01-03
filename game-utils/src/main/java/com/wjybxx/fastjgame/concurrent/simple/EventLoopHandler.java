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

package com.wjybxx.fastjgame.concurrent.simple;

import com.wjybxx.fastjgame.concurrent.EventLoop;

/**
 * 事件循环策略 - 将线程管理代理与非线程代码分隔。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/1/2
 * github - https://github.com/hl845740757
 */
public interface EventLoopHandler {

    /**
     * 执行需要的初始化。
     * 调用时机：EventLoop线程在启动时，会调用该方法。
     *
     * @param eventLoop EventHandler所在的EventLoop，你可以保留该引用。
     * @throws Exception error
     * @apiNote 如果抛出任何异常，将导致EventLoop退出
     */
    void init(EventLoop eventLoop) throws Exception;

    /**
     * 执行一次循环。
     * 调用时机：EventLoop线程会在每帧调用该方法 - 注意，帧间隔由{@link EventLoopHandler}自己控制。
     *
     * @throws Exception error
     */
    void loopOnce() throws Exception;

    /**
     * 清理{@link EventLoopHandler}持有的资源。
     * 调用时机：EventLoop线程会在线程退出时调用该方法。
     *
     * @throws Exception error
     */
    void clean() throws Exception;

    /**
     * 唤醒EventLoop线程，
     *
     * @param eventLoop   EventHandler所在的EventLoop，使用该参数可避免你保证EventLoop的可见性。
     * @param interrupter 如果需要中断唤醒EventLoop的话，可用于中断{@link EventLoop}线程。
     *                    如果线程阻塞在别的地方，你必须实现自己的唤醒策略。
     * @apiNote 该方法由其它线程调用，需要注意线程安全问题。
     */
    void wakeUpEventLoop(EventLoop eventLoop, EventLoopThreadInterrupter interrupter);

    /**
     * 当在刷帧时抛出异常该方法将被调用。
     *
     * @param cause {@link #loopOnce()}抛出的异常
     */
    void onLoopOnceExceptionCaught(Throwable cause);
}
