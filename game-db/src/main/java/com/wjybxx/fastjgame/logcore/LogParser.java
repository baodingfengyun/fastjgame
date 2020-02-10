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

package com.wjybxx.fastjgame.logcore;

/**
 * 日志解析器。
 * {@link #parse(Object)}负责将存储在'仓库'中存储的日志转换为应用程序使用的私有日志类，以去除对存储细节的依赖。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/10
 * github - https://github.com/hl845740757
 */
public interface LogParser<T, R> {

    /**
     * 将拉取到的日志数据解析为适合应用程序处理的日志记录类。
     *
     * @param storedData 仓库存储的数据
     * @return 适合应用程序处理的日志记录类
     */
    R parse(T storedData);

}
