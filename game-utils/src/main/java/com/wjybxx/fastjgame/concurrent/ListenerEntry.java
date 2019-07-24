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

package com.wjybxx.fastjgame.concurrent;

import javax.annotation.Nullable;

/**
 * 单个监听器的执行信息.
 * POJO
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/14 15:10
 * github - https://github.com/hl845740757
 */
public class ListenerEntry<V> {

    /**
     * 监听器
     */
    public final FutureListener<V> listener;

    /**
     * 监听器的执行环境，如果未指定，则使用运行环境下特定的executor。
     */
    @Nullable
    public final EventLoop bindExecutor;

    public ListenerEntry(FutureListener<V> listener, EventLoop bindExecutor) {
        this.listener = listener;
        this.bindExecutor = bindExecutor;
    }
}
