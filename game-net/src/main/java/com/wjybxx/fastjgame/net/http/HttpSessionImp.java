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

package com.wjybxx.fastjgame.net.http;

import com.wjybxx.fastjgame.concurrent.EventLoop;
import com.wjybxx.fastjgame.concurrent.ListenableFuture;
import com.wjybxx.fastjgame.concurrent.adapter.NettyListenableFutureAdapter;
import com.wjybxx.fastjgame.eventloop.NetEventLoop;
import com.wjybxx.fastjgame.manager.HttpSessionManager;
import com.wjybxx.fastjgame.utils.ConcurrentUtils;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpResponse;

/**
 * http会话信息
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/4/29 9:49
 * github - https://github.com/hl845740757
 */
public final class HttpSessionImp implements HttpSession {

    private final EventLoop localEventLoop;
    private final NetEventLoop netEventLoop;
    private final HttpSessionManager httpSessionManager;
    private final Channel channel;

    public HttpSessionImp(EventLoop localEventLoop, NetEventLoop netEventLoop, HttpSessionManager httpSessionManager, Channel channel) {
        this.localEventLoop = localEventLoop;
        this.netEventLoop = netEventLoop;
        this.httpSessionManager = httpSessionManager;
        this.channel = channel;
    }

    public Channel getChannel() {
        return channel;
    }

    @Override
    public boolean isActive() {
        return channel.isActive();
    }

    public ListenableFuture<?> writeAndFlush(HttpResponse response) {
        return new NettyListenableFutureAdapter<>(localEventLoop(), channel.writeAndFlush(response));
    }

    public <T extends HttpResponseBuilder<T>> ListenableFuture<?> writeAndFlush(HttpResponseBuilder<T> builder) {
        return writeAndFlush(builder.build());
    }

    @Override
    public ListenableFuture<?> close() {
        channel.close();
        return ConcurrentUtils.submitOrRun(netEventLoop(), () -> {
            httpSessionManager.removeSession(channel);
        });
    }

    @Override
    public NetEventLoop netEventLoop() {
        return netEventLoop;
    }

    @Override
    public EventLoop localEventLoop() {
        return localEventLoop;
    }
}
