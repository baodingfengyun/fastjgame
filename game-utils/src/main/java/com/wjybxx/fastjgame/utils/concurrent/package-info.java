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

/**
 * 并发工具包，游戏多线程化的方向。
 * 整体来说是参考的Netty的EventLoop实现，Netty的并发包较为庞大，支持的东西较多，
 * 这里只取需要的部分，将其简化。有部分类就是Netty的，之所以进行拷贝，是因为希望这个包不引入Netty。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/13 0:40
 * github - https://github.com/hl845740757
 */
package com.wjybxx.fastjgame.utils.concurrent;