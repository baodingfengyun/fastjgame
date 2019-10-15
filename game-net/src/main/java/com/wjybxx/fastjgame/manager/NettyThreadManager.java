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

package com.wjybxx.fastjgame.manager;

import com.google.inject.Inject;
import com.wjybxx.fastjgame.concurrent.DefaultThreadFactory;
import com.wjybxx.fastjgame.misc.HostAndPort;
import com.wjybxx.fastjgame.misc.PortRange;
import com.wjybxx.fastjgame.net.socket.DefaultSocketPort;
import com.wjybxx.fastjgame.utils.NetUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.BindException;

/**
 * Netty线程管理器。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/4/29 20:02
 * github - https://github.com/hl845740757
 */
public class NettyThreadManager {

    private static final Logger logger = LoggerFactory.getLogger(NettyThreadManager.class);
    /**
     * 开启限流，多分配一点空间，否则容易被限流，导致数据丢失。
     * 发送消息时判断{@link Channel#isWritable()}
     * （默认流量的8倍）
     */
    private static final WriteBufferWaterMark WRITE_BUFFER_WATER_MARK = new WriteBufferWaterMark(8 * 32 * 1024, 8 * 64 * 1024);

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    @Inject
    public NettyThreadManager() {

    }

    public void init(int bossGroupThreadNum, int workerGroupThreadNum) {
        if (bossGroup != null) {
            // 非法调用
            throw new IllegalStateException();
        }
        bossGroup = new NioEventLoopGroup(bossGroupThreadNum, new DefaultThreadFactory("ACCEPTOR_THREAD"));
        workerGroup = new NioEventLoopGroup(workerGroupThreadNum, new DefaultThreadFactory("WORKER_THREAD"));
    }

    /**
     * 关闭Netty的线程
     */
    public void shutdown() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
        logger.info("NettyThreadManager shutdown success");
    }

    /**
     * 监听某个端口,阻塞直到成功或失败。
     * 参数意义可参考{@link java.net.StandardSocketOptions}
     * 或 - https://www.cnblogs.com/googlemeoften/p/6082785.html
     *
     * @param host        地址
     * @param port        需要绑定的端口
     * @param sndBuffer   socket发送缓冲区
     * @param rcvBuffer   socket接收缓冲区
     * @param initializer channel初始化类，根据使用的协议(eg:tcp,ws) 和 序列化方式(eg:json,protoBuf)确定
     * @return 监听成功成功则返回绑定的地址，失败则返回null
     */
    public DefaultSocketPort bind(String host, int port, int sndBuffer, int rcvBuffer, ChannelInitializer<SocketChannel> initializer) throws BindException {
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(bossGroup, workerGroup);

        serverBootstrap.channel(NioServerSocketChannel.class);
        serverBootstrap.childHandler(initializer);

        // parentGroup参数
        serverBootstrap.option(ChannelOption.SO_BACKLOG, 1024);
        serverBootstrap.option(ChannelOption.SO_REUSEADDR, true);
        serverBootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        serverBootstrap.option(ChannelOption.WRITE_BUFFER_WATER_MARK, WRITE_BUFFER_WATER_MARK);

        // childGroup参数
        serverBootstrap.childOption(ChannelOption.SO_KEEPALIVE, false);
        serverBootstrap.childOption(ChannelOption.TCP_NODELAY, true);
        serverBootstrap.childOption(ChannelOption.SO_SNDBUF, sndBuffer);
        serverBootstrap.childOption(ChannelOption.SO_RCVBUF, rcvBuffer);
        serverBootstrap.childOption(ChannelOption.SO_LINGER, 0);
        serverBootstrap.childOption(ChannelOption.SO_REUSEADDR, true);
        serverBootstrap.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        serverBootstrap.childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, WRITE_BUFFER_WATER_MARK);

        ChannelFuture channelFuture = serverBootstrap.bind(host, port);
        try {
            channelFuture.sync();
            logger.info("bind {}:{} success.", host, port);
            return new DefaultSocketPort(channelFuture.channel(), new HostAndPort(host, port));
        } catch (InterruptedException e) {
            // ignore e
            NetUtils.closeQuietly(channelFuture);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            // ignore, may another process bind this port
            NetUtils.closeQuietly(channelFuture);
        }
        throw new BindException("can't bind " + host + ":" + port);
    }

    /**
     * 在某个端口范围内选择一个端口监听.
     *
     * @param host        地址
     * @param portRange   端口范围
     * @param sndBuffer   socket发送缓冲区
     * @param rcvBuffer   socket接收缓冲区
     * @param initializer channel初始化类
     * @return 监听成功的端口号，失败返回null
     */
    public DefaultSocketPort bindRange(String host, PortRange portRange, int sndBuffer, int rcvBuffer, ChannelInitializer<SocketChannel> initializer) throws BindException {
        if (portRange.startPort <= 0) {
            throw new IllegalArgumentException("fromPort " + portRange.startPort);
        }
        if (portRange.startPort > portRange.endPort) {
            throw new IllegalArgumentException("fromPort " + portRange.startPort + " toPort " + portRange.endPort);
        }
        for (int port = portRange.startPort; port <= portRange.endPort; port++) {
            try {
                return bind(host, port, sndBuffer, rcvBuffer, initializer);
            } catch (BindException e) {
                // ignore
            }
        }
        throw new BindException("can't bind port from " + portRange.startPort + " to " + portRange.endPort);
    }

    /**
     * 异步建立连接localHost
     *
     * @param hostAndPort      服务器地址
     * @param sndBuffer        socket发送缓冲区
     * @param rcvBuffer        socket接收缓冲区
     * @param connectTimeoutMs 建立连接超时时间
     * @param initializer      channel初始化类，根据使用的协议(eg:tcp,ws) 和 序列化方式(eg:json,protoBuf)确定
     * @return channelFuture 注意使用{@link ChannelFuture#sync()} 会抛出异常。
     * 使用{@link ChannelFuture#await()} 和{@link ChannelFuture#isSuccess()} 安全处理。
     * 此外，使用channel 需要调用 {@link Channel#isActive()}检查是否成功和远程建立连接
     */
    public ChannelFuture connectAsyn(HostAndPort hostAndPort, int sndBuffer, int rcvBuffer, int connectTimeoutMs,
                                     ChannelInitializer<SocketChannel> initializer) {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(workerGroup);

        bootstrap.channel(NioSocketChannel.class);
        bootstrap.handler(initializer);

        bootstrap.option(ChannelOption.SO_KEEPALIVE, false);
        bootstrap.option(ChannelOption.TCP_NODELAY, true);
        bootstrap.option(ChannelOption.SO_SNDBUF, sndBuffer);
        bootstrap.option(ChannelOption.SO_RCVBUF, rcvBuffer);
        bootstrap.option(ChannelOption.SO_LINGER, 0);
        bootstrap.option(ChannelOption.SO_REUSEADDR, true);
        bootstrap.option(ChannelOption.WRITE_BUFFER_WATER_MARK, WRITE_BUFFER_WATER_MARK);
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs);
        return bootstrap.connect(hostAndPort.getHost(), hostAndPort.getPort());
    }

    /**
     * 同步建立连接
     *
     * @param hostAndPort      服务器地址
     * @param sndBuffer        socket发送缓冲区
     * @param rcvBuffer        socket接收缓冲区
     * @param connectTimeoutMs 建立连接超时时间
     * @param initializer      channel初始化类，根据使用的协议(eg:tcp,ws) 和 序列化方式(eg:json,protoBuf)确定
     * @return 注意！使用channel 需要调用 {@link Channel#isActive()}检查是否成功和远程建立连接
     */
    public Channel connectSyn(HostAndPort hostAndPort, int sndBuffer, int rcvBuffer, int connectTimeoutMs,
                              ChannelInitializer<SocketChannel> initializer) {
        ChannelFuture channelFuture = connectAsyn(hostAndPort, sndBuffer, rcvBuffer, connectTimeoutMs, initializer);
        channelFuture.awaitUninterruptibly();
        return channelFuture.channel();
    }
}
