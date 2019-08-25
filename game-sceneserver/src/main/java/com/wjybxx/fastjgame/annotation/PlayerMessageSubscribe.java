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

package com.wjybxx.fastjgame.annotation;

/**
 * 玩家消息订阅者，表示处理玩家发来的该类型消息。<br>
 * 要使用该注解，方法必须满足以下条件：
 * <li>1. 函数必须是两个参数：第一个必须Player类型参数，第二个参数为具体消息类型参数。</li>
 * <li>2. 必须是public </li>
 * <li>3. 返回值类型必须是void</li>
 * <li>4. 该注解只能在scene模块使用</li>
 * 否则编译时会报错。
 * <pre>
 * {@code
 *      public void onMessage(Player player, ConcreteMessage message) {
 *          // do something
 *      }
 * }
 * </pre>
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/25
 * github - https://github.com/hl845740757
 */
public @interface PlayerMessageSubscribe {

}
