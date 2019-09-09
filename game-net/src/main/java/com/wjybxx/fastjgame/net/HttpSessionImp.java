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

import com.wjybxx.fastjgame.concurrent.ListenableFuture;
import com.wjybxx.fastjgame.manager.HttpSessionManager;
import com.wjybxx.fastjgame.misc.HostAndPort;
import com.wjybxx.fastjgame.misc.HttpResponseBuilder;
import com.wjybxx.fastjgame.utils.EventLoopUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.http.HttpResponse;

import javax.annotation.Nonnull;
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
    private final HostAndPort localAddress;
    private final HttpSessionManager httpSessionManager;
    /**
     * session对应的channel
     */
    private final Channel channel;
    /**
     * 是否是激活状态
     */
    private final AtomicBoolean stateHolder = new AtomicBoolean(true);

    public HttpSessionImp(NetContext netContext, HostAndPort localAddress, HttpSessionManager httpSessionManager, Channel channel) {
        this.netContext = netContext;
        this.localAddress = localAddress;
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

    @Nonnull
    @Override
    public HostAndPort localAddress() {
        return localAddress;
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

    /**
     * 发送一个响应
     *
     * @param response 响应内容
     * @return 注意相同的警告，建议使用{@link ChannelFuture#await()} 和{@link ChannelFuture#isSuccess()}
     * 代替{@link ChannelFuture#sync()}
     */
    public ChannelFuture writeAndFlush(HttpResponse response) {
        return channel.writeAndFlush(response);
    }

    /**
     * 发送一个响应
     *
     * @param <T>     builder自身
     * @param builder 建造者
     * @return 注意相同的警告，建议使用{@link ChannelFuture#await()} 和{@link ChannelFuture#isSuccess()}
     * 代替{@link ChannelFuture#sync()}
     */
    public <T extends HttpResponseBuilder<T>> ChannelFuture writeAndFlush(HttpResponseBuilder<T> builder) {
        HttpResponse response = builder.build();
        return writeAndFlush(response);
    }

    @Override
    public ListenableFuture<?> close() {
        setClosed();
        return EventLoopUtils.submitOrRun(netContext.netEventLoop(), () -> {
            httpSessionManager.removeSession(this, channel);
        });
    }
}
