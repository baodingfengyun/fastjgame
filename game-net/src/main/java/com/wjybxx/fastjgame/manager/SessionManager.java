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

package com.wjybxx.fastjgame.manager;

import com.wjybxx.fastjgame.concurrent.EventLoop;
import com.wjybxx.fastjgame.net.RpcCallback;
import com.wjybxx.fastjgame.net.RpcPromise;
import com.wjybxx.fastjgame.net.RpcResponse;

import javax.annotation.Nonnull;

/**
 * session管理器接口
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/9/9
 * github - https://github.com/hl845740757
 */
public interface SessionManager {

    /**
     * 发送一个单向消息到远程
     *
     * @param localGuid  我的标识
     * @param remoteGuid 远程节点标识
     * @param message    单向消息内容
     */
    void sendOneWayMessage(long localGuid, long remoteGuid, @Nonnull Object message);

    /**
     * 向远程发送一个异步rpc请求
     *
     * @param localGuid     我的标识
     * @param remoteGuid    远程节点标识
     * @param request       rpc请求内容
     * @param timeoutMs     超时时间，大于0
     * @param userEventLoop 用户线程
     * @param rpcCallback   rpc回调
     */
    void sendRpcRequest(long localGuid, long remoteGuid, @Nonnull Object request, long timeoutMs, EventLoop userEventLoop, RpcCallback rpcCallback);

    /**
     * 向远程发送一个同步rpc请求
     *
     * @param localGuid  我的标识
     * @param remoteGuid 远程节点标识
     * @param request    rpc请求内容
     * @param timeoutMs  超时时间，大于0
     * @param rpcPromise 用于监听结果
     */
    void sendSyncRpcRequest(long localGuid, long remoteGuid, @Nonnull Object request, long timeoutMs, RpcPromise rpcPromise);

    /**
     * 发送rpc响应
     *
     * @param localGuid   我的id
     * @param remoteGuid  远程节点id
     * @param requestGuid 请求对应的编号
     * @param sync        是否是同步rpc调用的结果
     * @param response    响应结果
     */
    void sendRpcResponse(long localGuid, long remoteGuid, long requestGuid, boolean sync, @Nonnull RpcResponse response);

    /**
     * 删除指定session
     *
     * @param localGuid  我的id
     * @param remoteGuid 远程节点id
     * @param reason     删除会话的原因
     * @return 删除成功，或节点已删除，则返回true。
     */
    boolean removeSession(long localGuid, long remoteGuid, String reason);

    /**
     * 删除指定用户的所有session
     *
     * @param localGuid 我的id
     * @param reason    删除会话的原因
     */
    void removeUserSession(long localGuid, String reason);

    /**
     * 当检测到用户所在的线程终止(注册NetContext的用户)
     *
     * @param userEventLoop 用户所在的EventLoop
     */
    void onUserEventLoopTerminal(EventLoop userEventLoop);

}
