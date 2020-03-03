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

package com.wjybxx.fastjgame.net.serialization;

/**
 * 消息映射策略，自己决定消息类到消息id的映射。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/5 17:21
 * github - https://github.com/hl845740757
 * @apiNote 1. 必须保证同一个类在所有机器上的映射结果是相同的。
 */
public interface MessageMappingStrategy {

    /**
     * 对实体进行映射，将实体映射为唯一id。
     * 它的主要作用是减少传输量和编解码效率（字符串传输量大，且hash和equals开销大 -- 每次读入都是一个新的字符串，开销极大）。
     * 用户可以对实体名进行过滤，从返回结果中删除，表示不支持该实体序列化。
     */
    int mapping(Class<?> messageClass);

}
