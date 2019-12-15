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

package com.wjybxx.fastjgame.kafka;

import org.apache.kafka.clients.producer.ProducerRecord;

import javax.annotation.Nonnull;

/**
 * 日志建造指挥官，构建最终的日志内容。
 * Q: 为什么这里的方法不定义在{@link LogBuilder}中？
 * A: 线程切换更加明确，可简化开发难度，更容易保证正确性。
 * 此外，也使得{@link LogDirector}和{@link LogBuilder}各自的改变和优化更为容易。
 * <p>
 * 注意：该对象内的方法执行在kafka线程，而{@link LogBuilder}内的方法执行在应用线程。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/11/30
 * github - https://github.com/hl845740757
 */
public interface LogDirector<T extends LogBuilder> {

    /**
     * 构建日志内容
     *
     * @param builder 含有日志内容的builder
     * @return 传输的内容 - 目前先限定为字符串键值对
     */
    @Nonnull
    ProducerRecord<String, String> build(T builder);

}
