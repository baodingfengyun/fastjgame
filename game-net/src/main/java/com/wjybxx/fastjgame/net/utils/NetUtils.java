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

package com.wjybxx.fastjgame.net.utils;

import com.wjybxx.fastjgame.net.exception.RpcTimeoutException;
import com.wjybxx.fastjgame.utils.CloseableUtils;
import com.wjybxx.fastjgame.utils.annotation.UnstableApi;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Closeable;
import java.net.*;
import java.net.http.HttpTimeoutException;
import java.util.Enumeration;

/**
 * 网络包工具类
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/4/27 10:28
 * github - https://github.com/hl845740757
 */
public class NetUtils {

    private static final Logger logger = LoggerFactory.getLogger(NetUtils.class);

    /**
     * 读超时handler的名字
     */
    public static final String READ_TIMEOUT_HANDLER_NAME = "readTimeoutHandler";
    /**
     * 默认读超时时间
     */
    public static final int DEFAULT_READ_TIMEOUT = 45;

    /**
     * 本机内网地址
     */
    private static volatile String localIp;
    /**
     * 本机外网地址
     */
    private static volatile String outerIp;
    /**
     * 请求图标的路径
     */
    public static final String FAVICON_PATH = "/favicon.ico";

    static {
        // 尝试查找一下内外网ip
        localIp = findLocalIp();
        outerIp = findOuterIp();

        logger.info("localIp {}", localIp);
        logger.info("outerIp {}", outerIp);
    }

    // close
    private NetUtils() {

    }

    /**
     * 计算sessionId对应的固定的key
     *
     * @param sessionId 待计算的sessionId
     * @return 对于同一个sessionId，它计算得到的key是不变的
     */
    public static int fixedKey(String sessionId) {
        return sessionId.hashCode();
    }

    /**
     * 计算Channel对应的固定的key
     *
     * @param channel 待计算的channel
     * @return 对于同一个Channel，它计算得到的key是不变的
     */
    public static int fixedKey(Channel channel) {
        return channel.id().hashCode();
    }

    /**
     * 安静的关闭channel,不产生任何影响
     */
    public static void closeQuietly(@Nullable Channel channel) {
        if (null != channel) {
            try {
                channel.close();
            } catch (Throwable ignore) {

            }
        }
    }

    /**
     * 安静的关闭future，不产生任何影响
     */
    public static void closeQuietly(@Nullable ChannelFuture channelFuture) {
        if (null != channelFuture) {
            try {
                channelFuture.cancel(true);
                channelFuture.channel().close();
            } catch (Throwable ignore) {

            }
        }
    }

    /**
     * 安静的关闭ctx，不产生任何影响
     */
    public static void closeQuietly(@Nullable ChannelHandlerContext ctx) {
        if (null != ctx) {
            try {
                ctx.close();
            } catch (Throwable ignore) {

            }
        }
    }

    /**
     * 关闭一个资源
     */
    public static void closeQuietly(@Nullable Closeable resource) {
        CloseableUtils.closeQuietly(resource);
    }

    /**
     * 设置内网Ip
     *
     * @param localIp ip
     */
    public static void setLocalIp(@Nonnull String localIp) {
        NetUtils.localIp = localIp;
    }

    /**
     * 设置外网ip
     *
     * @param outerIp ip
     */
    public static void setOuterIp(@Nonnull String outerIp) {
        NetUtils.outerIp = outerIp;
    }

    /**
     * 获取机器内网ip
     *
     * @return 内网地址
     */
    @UnstableApi
    public static String getLocalIp() {
        return localIp;
    }

    /**
     * 获取机器外网ip
     *
     * @return 如果无法获取到外网地址，返回的是内网地址
     */
    @UnstableApi
    public static String getOuterIp() {
        return outerIp;
    }

    /**
     * 查找内网ip。
     *
     * @return localIp
     */
    private static String findLocalIp() {
        String hostAddress = "127.0.0.1";
        try {
            hostAddress = Inet4Address.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            logger.error("get localHost caught exception,use {} instead.", hostAddress, e);
        }
        return hostAddress;
    }

    /**
     * 获取本机外网ip，如果不存在则返回内网ip。
     * <p>
     * 个人对此不是很熟，参考自以下文章
     * - http://www.voidcn.com/article/p-rludpgmk-yb.html
     * - https://rainbow702.iteye.com/blog/2066431
     * <p>
     * {@link Inet4Address#isSiteLocalAddress()} 是否是私有网段
     * 10/8 prefix ，172.16/12 prefix ，192.168/16 prefix
     * {@link Inet4Address#isLoopbackAddress()} 是否是本机回环ip (127.x.x.x)
     * {@link Inet4Address#isAnyLocalAddress()} 是否是通配符地址 (0.0.0.0)
     *
     * @return outerIp
     */
    private static String findOuterIp() {
        try {
            Enumeration<NetworkInterface> netInterfaces = NetworkInterface.getNetworkInterfaces();
            while (netInterfaces.hasMoreElements()) {
                NetworkInterface ni = netInterfaces.nextElement();
                Enumeration<InetAddress> address = ni.getInetAddresses();
                while (address.hasMoreElements()) {
                    InetAddress ip = address.nextElement();
                    if (!(ip instanceof Inet4Address)) {
                        continue;
                    }
                    // 回环地址
                    Inet4Address inet4Address = (Inet4Address) ip;
                    if (inet4Address.isLoopbackAddress()) {
                        continue;
                    }
                    // 通配符地址
                    if (inet4Address.isAnyLocalAddress()) {
                        continue;
                    }
                    // 私有地址(内网地址)
                    if (ip.isSiteLocalAddress()) {
                        continue;
                    }
                    return inet4Address.getHostAddress();
                }
            }
        } catch (SocketException e) {
            logger.info("find outerIp caught exception,will use localIp instead.", e);
            return findLocalIp();
        }
        logger.info("can't find outerIp, will use localIp instead.");
        return findLocalIp();
    }

    public static void main(String[] args) {
        System.out.println("localIp " + localIp);
        System.out.println("outerIp " + outerIp);
    }

}
