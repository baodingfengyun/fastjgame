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

import javax.annotation.concurrent.NotThreadSafe;

/**
 * 日志构建器 - 该对象用于搜集日志数据。
 * 这是一个窄接口，不限制api，kafka线程通过{@link LogDirector}获取日志内容。
 * <p>
 * 注意：
 * 1. 添加完数据之后，调用{@link LogProducerEventLoop#publish(LogBuilder)}发布自己。
 * 2. 每条日志务必使用新的对象，发布之后再修改会导致线程安全问题。
 * 3. 该对象内的方法执行在应用线程，而{@link LogDirector}内的方法执行在kafka线程。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/12/15
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public interface LogBuilder {

}
