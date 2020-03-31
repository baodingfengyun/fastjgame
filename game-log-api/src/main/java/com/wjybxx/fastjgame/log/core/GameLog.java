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
 * 游戏日志，这是一个标记接口，应用为每一种类型的日志声明一个具体的类型。
 * 如: 升级日志、登录日志、登出日志。
 * <p>
 * Q: 为什么使用该接口代替了之前的Builder接口？
 * A: Builder搜集日志确实很方便，但缺乏表达力，会增加维护难度，不如每一种类型的日志定义一个具体的类清晰。
 * 此外，该接口实际并未限制你使用Builder模式，如果你需要，仍可以使用Builder模式。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/3/31
 * github - https://github.com/hl845740757
 */
public interface GameLog {

}
