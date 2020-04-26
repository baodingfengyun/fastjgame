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

package com.wjybxx.fastjgame.utils.concurrent;


import javax.annotation.Nonnull;
import java.util.concurrent.*;

/**
 * 它继承了JDK的{@link Future}，出现了阻塞式api。
 * <h3>建议</h3>
 * 如果不是必须需要阻塞式的API，应当优先选择{@link FluentFuture}。
 *
 * @param <V> 值类型
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/14
 * github - https://github.com/hl845740757
 */
public interface BlockingFuture<V> extends Future<V>, FluentFuture<V> {

    // ------------------------------------- 阻塞式获取操作结果 ---------------------------------------



    // -------------------------------- 阻塞式等待future进入完成状态  --------------------------------------

}
