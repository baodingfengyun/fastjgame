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

package com.wjybxx.fastjgame.net.local;

import com.wjybxx.fastjgame.concurrent.ListenableFuture;
import com.wjybxx.fastjgame.eventloop.NetContext;
import com.wjybxx.fastjgame.misc.NetPort;
import com.wjybxx.fastjgame.net.session.Session;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * 用于建立JVM内部session的“端口”，它并非一个真正的端口。
 * 注意：每次调用{@link NetContext#bindLocal(LocalSessionConfig)}都会产生一个新的{@link LocalPort}。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/9/9
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public interface LocalPort extends NetPort {

    /**
     * 连接到该{@link LocalPort}上。
     * 这样设计的目的：对用户屏蔽底层实现。
     *
     * @param netContext 用户所属的网络环境
     * @param config     session 配置新
     * @return future
     */
    ListenableFuture<Session> connect(@Nonnull NetContext netContext, @Nonnull LocalSessionConfig config);

}
