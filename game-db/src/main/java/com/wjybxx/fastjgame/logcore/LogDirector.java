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

package com.wjybxx.fastjgame.logcore;

import javax.annotation.Nonnull;

/**
 * 日志建造指挥官，构建最终的日志内容。
 * Q: 为什么这里的方法不定义在{@link LogBuilder}中？
 * A: 1. 搜集日志内容的方式变化的可能性较小，而构建最终发布内容的方式变化的可能性较大，我们需要将不同变化频率的逻辑隔离。
 * 2. 可能需要支持多种内容格式(常见) 或 多种发布方式(不常见)，那么就需要不同的构建方式。
 * 3. 可以将耗时操作转移到非逻辑线程，这样的设计可以使得线程切换更加明确，可简化开发难度，更容易保证正确性。
 * 4. 也使得{@link LogDirector}和{@link LogBuilder}各自的改变和优化更为容易。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/11/30
 * github - https://github.com/hl845740757
 */
public interface LogDirector<T extends LogBuilder, R> {

    /**
     * 构建日志内容
     *
     * @param builder 含有日志内容的builder
     * @return 用于发布的内容
     */
    @Nonnull
    R build(T builder);

}
