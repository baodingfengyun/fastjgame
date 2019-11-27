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

package com.wjybxx.fastjgame.eventbus;

import com.wjybxx.fastjgame.annotation.UnstableApi;

import javax.annotation.Nonnull;

/**
 * 事件分发器
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/27
 * github - https://github.com/hl845740757
 */
public interface EventDispatcher {

    /**
     * 发布一个事件，以{@code event.getClass()}作为事件类型。
     *
     * @param event 要发布的事件
     * @param <T>   事件的类型
     */
    <T> void post(@Nonnull T event);

    /**
     * 发布一个事件，并指定触发的事件类型
     *
     * @param keyClazz 希望事件以某个类型被处理。手动指定更加灵活，否则每次过滤筛选，效率差还容易造成错误。
     * @param event    要发布的事件
     * @param <T>      事件的类型
     */
    @UnstableApi
    <T> void post(Class<? super T> keyClazz, @Nonnull T event);
}
