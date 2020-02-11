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

package com.wjybxx.fastjgame.core;

/**
 * 日志解析器。
 * {@link #parse(LogRecordDTO)}负责将存储在'仓库'中存储的日志转换为应用程序使用的私有日志类，以去除对存储细节的依赖。
 * <p>
 * 日志解析是细节，业务逻辑应该不关心该实现。
 * <p>
 * Q: 为什么这里的方法不定义在{@link LogConsumer}中？
 * 1. 日志解析和消费日志运行在不同线程，这样的设计可以使得线程切换更加明确，可简化开发难度，更容易保证正确性。
 * {@link LogConsumer}运行在用户指定线程，而日志解析运行在{@link LogPuller}线程，存在线程切换。
 * 2. 日志解析和消费日志操作变更的频率和原因不同，我们需要将不同变更频率/变更原因的逻辑隔离。
 * 3. 两个操作各自的改变和优化更为容易。
 * 4. 可以将耗时操作转移到非逻辑线程。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/10
 * github - https://github.com/hl845740757
 */
public interface LogParser<VO> {

    /**
     * 解析{@link LogPuller}拉取的日志，用于{@link LogConsumer}消费。
     *
     * @param recordDTO 仓库存储的数据（DTO）
     * @return 适合应用程序处理的日志记录类（VO）
     */
    VO parse(LogRecordDTO recordDTO);

}
