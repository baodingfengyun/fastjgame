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

package com.wjybxx.fastjgame.kafka;

import com.wjybxx.fastjgame.core.LogParser;
import org.apache.kafka.clients.consumer.ConsumerRecord;

/**
 * kafka日志解析器
 * 将kafka中存储的日志格式转换为应用程序需要的日志类型。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/10
 * github - https://github.com/hl845740757
 */
public interface KafkaLogParser<R> extends LogParser<ConsumerRecord<String, String>, R> {

    @Override
    R parse(ConsumerRecord<String, String> storedData);

}
