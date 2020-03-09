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

import com.wjybxx.fastjgame.net.eventloop.NetEventLoop;
import com.wjybxx.fastjgame.net.misc.NetContext;
import com.wjybxx.fastjgame.utils.concurrent.EventLoop;
import com.wjybxx.fastjgame.utils.concurrent.ListenableFuture;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpResponse;

/**
 * HttpSession抽象接口
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/3
 * github - https://github.com/hl845740757
 */
public interface HttpSession {

    /**
     * 该session所在的NetEventLoop。
     */
    NetEventLoop netEventLoop();

    /**
     * 该session所在的用户线程，一定是{@link NetContext#appEventLoop()}
     */
    EventLoop appEventLoop();

    /**
     * 该session关联的netty的{@link Channel}
     */
    Channel channel();

    /**
     * session是否处于活动状态
     *
     * @return true/false
     */
    boolean isActive();

    /**
     * 关闭session。
     * 子类实现必须线程安全。
     */
    ListenableFuture<?> close();

    /**
     * 发送一个响应
     *
     * @param response 响应内容
     * @return future，该future的回调执行环境为用户线程。
     */
    ListenableFuture<?> writeAndFlush(HttpResponse response);

    /**
     * 发送一个http结果对象
     *
     * @param <T>     builder自身
     * @param builder 建造者
     * @return future，该future的回调执行环境为用户线程。
     */
    <T extends HttpResponseBuilder<T>> ListenableFuture<?> writeAndFlush(HttpResponseBuilder<T> builder);
}
