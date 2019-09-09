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

package com.wjybxx.fastjgame.net;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Session发送消息的真正实现。sender负责将消息传输到网络层(UserEventLoop -> NetEventLoop)。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/30
 * github - https://github.com/hl845740757
 * @apiNote 实现类必须是线程安全的。
 */
@ThreadSafe
public interface Sender {

    /**
     * 返回sender关联的session
     */
    Session session();

    /**
     * 发送一个单向消息/通知
     *
     * @param message 待发送的消息
     */
    void send(@Nonnull Object message);

    /**
     * 发送一个**异步**rpc请求给对方，并阻塞到结果返回或超时或被中断。
     * 注意：回调执行在session的用户线程。
     *
     * @param request   rpc请求对象
     * @param callback  回调函数
     * @param timeoutMs 超时时间，毫秒，必须大于0，必须有超时时间。
     */
    void rpc(@Nonnull Object request, @Nonnull RpcCallback callback, long timeoutMs);

    /**
     * 发送一个**同步**rpc请求给对方，并阻塞到结果返回或超时或被中断。
     *
     * @param request   rpc请求对象
     * @param timeoutMs 超时时间，毫秒，必须大于0，否则死锁可能！！！
     * @return rpc返回结果
     */
    @Nonnull
    RpcResponse syncRpc(@Nonnull Object request, long timeoutMs) throws InterruptedException;

    /**
     * 发送一个**同步**rpc请求给对方，并阻塞到结果返回或超时。
     *
     * @param request   rpc请求对象
     * @param timeoutMs 超时时间，毫秒，必须大于0，否则死锁可能！！！
     * @return rpc返回结果
     */
    @Nonnull
    RpcResponse syncRpcUninterruptibly(@Nonnull Object request, long timeoutMs);

    /**
     * 创建一个特定rpc请求对应的结果通道。
     * {@link RpcResponseChannel}是线程安全的。
     *
     * @param requestGuid 请求对应的id
     * @param sync        是否是同步rpc调用
     * @return responseChannel
     */
    <T> RpcResponseChannel<T> newResponseChannel(long requestGuid, boolean sync);

    /**
     * 如果存在缓冲，则清空缓冲区。
     */
    void flush();

    /**
     * 关闭sender，也就是清空缓冲区中的所有消息
     */
    void clearBuffer();
}
