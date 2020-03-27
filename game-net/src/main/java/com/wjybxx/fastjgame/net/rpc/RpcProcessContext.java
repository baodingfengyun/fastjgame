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

package com.wjybxx.fastjgame.net.rpc;

import com.wjybxx.fastjgame.net.session.Session;

import javax.annotation.Nonnull;

/**
 * rpc执行时的上下文。
 * 定义该接口，方便扩展。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/3/27
 * github - https://github.com/hl845740757
 */
public interface RpcProcessContext {

    /**
     * 本次调用关联的session
     */
    @Nonnull
    Session session();

    /**
     * 是否是单项调用。
     * 如果是单项调用的话，表示不关心调用结果。
     */
    boolean isOneWay();

    /**
     * 如果是rpc调用时，返回该调用的唯一标识(该session下唯一)。
     * 如果不是rpc调用，则抛出异常。
     */
    long requestGuid();

    /**
     * 是否是同步rpc调用
     */
    boolean isSync();

}
