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

package com.wjybxx.fastjgame.net.socket;

import com.wjybxx.fastjgame.manager.NetEventManager;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

import javax.annotation.concurrent.ThreadSafe;

/**
 * 服务器channel初始化器示例
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/4/27 22:17
 * github - https://github.com/hl845740757
 */
@ThreadSafe
public class TCPServerChannelInitializer extends ChannelInitializer<SocketChannel> {

    /**
     * 本地发起监听的角色guid
     */
    private final long localGuid;
    private final SocketSessionConfig config;
    private final NetEventManager netEventManager;

    public TCPServerChannelInitializer(long localGuid, SocketSessionConfig config, NetEventManager netEventManager) {
        this.localGuid = localGuid;
        this.config = config;
        this.netEventManager = netEventManager;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new LengthFieldBasedFrameDecoder(config.maxFrameLength(), 0, 4, 0, 4));
        pipeline.addLast(new ServerSocketCodec(config.codec(), localGuid, config.lifecycleAware(), netEventManager));
    }
}
