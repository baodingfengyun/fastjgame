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

package com.wjybxx.fastjgame.world;

import com.wjybxx.fastjgame.concurrent.ListenableFuture;
import com.wjybxx.fastjgame.eventloop.NetEventLoopGroup;
import com.wjybxx.fastjgame.net.RoleType;

import javax.annotation.Nonnull;

/**
 * 抽象的游戏世界，顶层类
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/4
 * github - https://github.com/hl845740757
 */
public interface World {

    /**
     * world的guid
     */
    long worldGuid();

    /**
     * world代表的角色
     */
    @Nonnull
    RoleType worldRole();

    /**
     * 启动world，注意：world出现任何异常，都会停止执行并开始关闭
     *
     * @param gameEventLoop world所属的EventLoop，可以保存下来。
     * @param netEventLoopGroup 网络模块
     * @throws Exception errors
     */
    void startUp(GameEventLoop gameEventLoop, NetEventLoopGroup netEventLoopGroup) throws Exception;

    /**
     * 游戏世界刷帧
     * @param curTimeMills 当前时间戳
     */
    void tick(long curTimeMills);

    /**
     * 关闭world
     * @throws Exception errors
     */
    void shutdown() throws Exception;

    /**
     * 获取world绑定到的GameEventLoop
     * @return gameEventLoop
     */
    @Nonnull
    GameEventLoop gameEventLoop();

    /**
     * 从注册到的GameEventLoop上取消注册
     * @return future
     */
    ListenableFuture<?> deregister();
}
