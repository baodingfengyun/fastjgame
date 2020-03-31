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

package com.wjybxx.fastjgame.log.core;

/**
 * 日志解码器，将日志从仓库存储的格式解码为程序需要的数据格式。
 * 运行在{@link LogPublisher}线程
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/3/31
 * github - https://github.com/hl845740757
 */
public interface LogDecoder<T, R extends GameLog> {

    R decode(T record);

}
