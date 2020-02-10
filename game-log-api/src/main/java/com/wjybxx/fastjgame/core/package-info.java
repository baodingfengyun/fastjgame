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

/**
 * 对于逻辑用户而言：
 * 发布一条日志，使用{@link com.wjybxx.fastjgame.core.LogBuilder} 和 {@link com.wjybxx.fastjgame.core.LogPublisher}。
 * 消费一条日志，使用{@link com.wjybxx.fastjgame.core.LogConsumer}。
 * <p>
 * 对于架构程序员：
 * 需要设计应用私有的日志格式，并通过{@link com.wjybxx.fastjgame.core.LogParser} 和 {@link com.wjybxx.fastjgame.core.LogDirector}进行转换，
 * 以使得逻辑程序员脱离对具体日志组件的依赖。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/10
 * github - https://github.com/hl845740757
 */
package com.wjybxx.fastjgame.core;