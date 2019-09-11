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

package com.wjybxx.fastjgame.net;

import com.wjybxx.fastjgame.concurrent.EventLoop;
import com.wjybxx.fastjgame.concurrent.ListenableFuture;
import com.wjybxx.fastjgame.eventloop.NetEventLoop;
import com.wjybxx.fastjgame.manager.HttpSessionManager;
import com.wjybxx.fastjgame.misc.HttpResponseBuilder;
import com.wjybxx.fastjgame.utils.EventLoopUtils;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpResponse;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * http会话信息
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/4/29 9:49
 * github - https://github.com/hl845740757
 */
public final class HttpSessionImp implements HttpSession {

    private final NetContext netContext;
    private final HttpSessionManager httpSessionManager;
    /**
     * session对应的channel
     */
    private final Channel channel;
    /**
     * 是否是激活状态
     */
    private final AtomicBoolean stateHolder = new AtomicBoolean(true);

    public HttpSessionImp(NetContext netContext, HttpSessionManager httpSessionManager, Channel channel) {
        this.netContext = netContext;
        this.httpSessionManager = httpSessionManager;
        this.channel = channel;
    }

    @Override
    public long localGuid() {
        return netContext.localGuid();
    }

    @Override
    public RoleType localRole() {
        return netContext.localRole();
    }

    public Channel getChannel() {
        return channel;
    }

    @Override
    public boolean isAlive() {
        return stateHolder.get();
    }

    public void setClosed() {
        stateHolder.set(true);
    }

    public void writeAndFlush(HttpResponse response) {
        channel.writeAndFlush(response, channel.voidPromise());
    }

    public <T extends HttpResponseBuilder<T>> void writeAndFlush(HttpResponseBuilder<T> builder) {
        writeAndFlush(builder.build());
    }

    @Override
    public ListenableFuture<?> close() {
        setClosed();
        return EventLoopUtils.submitOrRun(netEventLoop(), () -> {
            httpSessionManager.removeSession(this, channel);
        });
    }

    @Override
    public NetEventLoop netEventLoop() {
        return netContext.netEventLoop();
    }

    @Override
    public EventLoop localEventLoop() {
        return netContext.localEventLoop();
    }
}
