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
 * 日志消费者。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/10
 * github - https://github.com/hl845740757
 */
public interface LogConsumer<T> {

    /**
     * 消费一条日志。
     *
     * @param record 日志数据
     */
    void consume(T record);

}
