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

package com.wjybxx.fastjgame.eventloop;

import com.wjybxx.fastjgame.concurrent.EventLoop;
import com.wjybxx.fastjgame.concurrent.EventLoopGroup;

import javax.annotation.Nonnull;

/**
 * 网络事件循环组
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/10/5
 * github - https://github.com/hl845740757
 */
public interface NetEventLoopGroup extends EventLoopGroup {

    /**
     * 创建一个网络上下文
     *
     * @param localEventLoop 方法的调用者所在的eventLoop
     * @return NetContext 创建的context可以用于监听，建立连接，和http请求
     */
    NetContext createContext(@Nonnull EventLoop localEventLoop);

    @Nonnull
    @Override
    NetEventLoop next();

    @Nonnull
    @Override
    NetEventLoop select(int key);

}
