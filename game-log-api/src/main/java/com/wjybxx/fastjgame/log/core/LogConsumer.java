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

package com.wjybxx.fastjgame.log.core;

import com.wjybxx.fastjgame.utils.concurrent.EventLoop;

import javax.annotation.Nullable;
import java.util.Set;

/**
 * 日志消费者。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/10
 * github - https://github.com/hl845740757
 */
public interface LogConsumer<T extends GameLog> {

    /**
     * 日志消费者的运行环境 - {@link #consume(GameLog)}的执行环境。
     * 如果返回null，表示直接在{@link LogPuller}线程消费。
     */
    @Nullable
    EventLoop appEventLoop();

    /**
     * 订阅的topic
     *
     * @apiNote 只在初始的时候使用一次，因此不必作为对象的属性，new一个即可。
     */
    Set<String> subscribedTopics();

    /**
     * 消费一条日志。
     *
     * @param gameLog 日志数据
     */
    void consume(T gameLog) throws Exception;

}
