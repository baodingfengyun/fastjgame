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

package com.wjybxx.fastjgame.misc.log;

import javax.annotation.Nonnull;

/**
 * 日志建造指挥官，构建最终的日志内容
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/11/30
 * github - https://github.com/hl845740757
 */
public interface LogDirector {

    /**
     * @param logBuilder    含有日志内容的builder
     * @param curTimeMillis 当前时间
     * @return 传输的内容
     */
    @Nonnull
    String build(LogBuilder logBuilder, long curTimeMillis);

    /**
     * 恢复到初始状态，如果{@link #build(LogBuilder, long)}修改了状态的话
     */
    void reset();
}
