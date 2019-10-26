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

import com.wjybxx.fastjgame.misc.PlayerMessageFunction;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 玩家消息订阅者，表示处理玩家发来的该类型消息。<br>
 * <p>
 * 方法必须满足以下要求，否则编译会报错：
 * <li>1. 函数必须是两个参数：第一个必须Player类型参数，第二个参数为具体消息类型参数。 也就是可以转换为{@link PlayerMessageFunction}</li>
 * <li>2. 方法不能是private - 至少是包级访问权限。 </li>
 * 否则编译时会报错。
 * <pre>{@code
 *      @PlayerMessageSubscribe
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
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface PlayerMessageSubscribe {

}
