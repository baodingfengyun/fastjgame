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

import javax.annotation.Nonnull;

/**
 * 日志建造指挥官。
 * 负责将应用程序搜集到的日志数据，转换为'仓库'中存储的格式，以去除搜集日志对'仓库'存储细节的依赖。
 * <p>
 * 日志构建是细节，业务逻辑应该不关心该实现
 * <p>
 * Q: 为什么这里的方法不定义在{@link LogBuilder}中？
 * A: 1. 日志的搜集过程与构建为传输对象在不同的线程执行，这样的设计可以使得线程切换更加明确，可简化开发难度，更容易保证正确性。
 * {@link LogBuilder}搜集日志数据过程在用户线程中执行，而{@link #build(LogBuilder)}执行在{@link LogPublisher}线程，存在线程切换。
 * 2. 搜集日志内容的方式变更的可能性较小，而构建最终发布内容的方式变更的可能性较大，我们需要将不同变更频率/变更原因的逻辑隔离。
 * eg: 发布到不同的存储组件时，很可能需要多种存储格式，那么就需要不同的构建方式。
 * 3. 两个操作各自的改变和优化更为容易。
 * 4. 可以将耗时操作转移到非逻辑线程。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/11/30
 * github - https://github.com/hl845740757
 */
public interface LogDirector<T extends LogBuilder, R> {

    /**
     * 构建日志内容，用于{@link LogPublisher}保存。
     *
     * @param builder 含有日志内容的builder
     * @return 方便仓库存储的记录格式
     */
    @Nonnull
    R build(T builder);

}
