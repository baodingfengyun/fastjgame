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
 * 日志发布器，它负责将日志发布到<b>某个地方</b>，如：kafka，本地文件，数据库，flume。
 * 定义该接口，可以使我们延迟做选择，并可以在不同的实现之间进行切换。
 * 此外，建议实现类使用{@link LogDirector}获取最终要发布的日志内容，以实现解耦(构建最终内容的业务逻辑是多变的)。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/9
 * github - https://github.com/hl845740757
 */
public interface LogPublisher<T extends LogBuilder> {

    /**
     * 发布一条日志
     *
     * @param logBuilder 含有日志内容的builder
     */
    void publish(T logBuilder);

}
