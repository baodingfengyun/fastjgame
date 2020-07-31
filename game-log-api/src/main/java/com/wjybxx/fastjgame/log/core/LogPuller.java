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

import com.wjybxx.fastjgame.util.concurrent.EventLoop;

/**
 * 日志拉取工具。
 * 它负责从'仓库'拉取消费者们感兴趣的日志。
 * <p>
 * Q: 为什么继承{@link EventLoop}？
 * A: 主要原因：我们需要能管理它的生命周期。
 * <p>
 * 构造方法应包含{@link LogDecoder} 和 {@link LogConsumer}集合。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/11
 * github - https://github.com/hl845740757
 */
public interface LogPuller extends EventLoop {

}
