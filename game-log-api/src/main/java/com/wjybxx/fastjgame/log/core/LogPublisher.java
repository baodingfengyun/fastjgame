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

/**
 * 日志发布器，它负责将日志发布到<b>某个地方</b>，如：kafka，本地文件，数据库，flume。
 * 定义该接口，可以使我们延迟做选择，并可以在不同的实现之间进行切换。
 * <p>
 * Q: 为什么继承{@link EventLoop}？
 * A: 主要原因：我们需要能管理它的生命周期。
 * <p>
 * 构造方法应包含{@link LogEncoder}。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/9
 * github - https://github.com/hl845740757
 */
public interface LogPublisher<T extends GameLog> extends EventLoop {

    /**
     * 发布一条日志
     *
     * @param gameLog 含有日志内容的builder
     */
    void publish(T gameLog);

}
