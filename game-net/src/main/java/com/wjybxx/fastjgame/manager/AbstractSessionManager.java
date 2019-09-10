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
import com.wjybxx.fastjgame.net.*;
import com.wjybxx.fastjgame.utils.ConcurrentUtils;
import com.wjybxx.fastjgame.utils.FastCollectionsUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;

/**
 * 实现session管理器的通用功能
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/9/9
 * github - https://github.com/hl845740757
 */
public abstract class AbstractSessionManager implements SessionManager {


    // ----------------------------------------------------------- 提交消息 --------------------------------------------------------------

    /**
     * 立即提交一个消息给应用层
     *
     * @param session    会话信息
     * @param commitTask 准备提交的消息
     */
    protected final void commit(Session session, CommitTask commitTask) {
        if (session.isActive()) {
            ConcurrentUtils.tryCommit(session.localEventLoop(), commitTask);
        }
        // else 丢弃
    }

    /**
     * 提交一个rpc响应结果。
     * rpc调用必须返回一个结果，但是会话关闭的情况下，不能提交真实结果。
     * why？
     * 因为在会话关闭的情况下，单向消息、rpc请求全部被丢弃了，如果提交真实的rpc响应，会导致应用层收到消息的顺序和发送方不一样！！！
     * session关闭的状态下，要么都提交，要么都不提交，不能选择性的提交。
     *
     * @param session        会话信息
     * @param rpcPromiseInfo rpc请求的一些信息
     * @param rpcResponse    rpc结果
     */
    protected final void commitRpcResponse(Session session, RpcPromiseInfo rpcPromiseInfo, RpcResponse rpcResponse) {
        if (rpcPromiseInfo.rpcPromise != null) {
            // 同步rpc调用
            if (session.isActive()) {
                rpcPromiseInfo.rpcPromise.trySuccess(rpcResponse);
            } else {
                rpcPromiseInfo.rpcPromise.trySuccess(RpcResponse.SESSION_CLOSED);
            }
        } else {
            RpcResponseCommitTask rpcResponseCommitTask;
            if (session.isActive()) {
                rpcResponseCommitTask = new RpcResponseCommitTask(rpcResponse, rpcPromiseInfo.rpcCallback);
            } else {
                rpcResponseCommitTask = new RpcResponseCommitTask(RpcResponse.SESSION_CLOSED, rpcPromiseInfo.rpcCallback);
            }
            ConcurrentUtils.tryCommit(session.localEventLoop(), rpcResponseCommitTask);
        }
    }


    /**
     * 清理Rpc请求信息
     *
     * @param session           会话信息
     * @param rpcPromiseInfoMap 未完成的Rpc请求
     */
    protected final void cleanRpcPromiseInfo(final Session session, final Long2ObjectMap<RpcPromiseInfo> rpcPromiseInfoMap) {
        if (rpcPromiseInfoMap.size() == 0) {
            return;
        }
        // 立即执行所有同步rpc调用
        FastCollectionsUtils.removeIfAndThen(rpcPromiseInfoMap,
                (k, rpcPromiseInfo) -> rpcPromiseInfo.rpcPromise != null,
                (k, rpcPromiseInfo) -> rpcPromiseInfo.rpcPromise.trySuccess(RpcResponse.SESSION_CLOSED));
        // 减少不必要的提交
        if (rpcPromiseInfoMap.size() == 0) {
            return;
        }
        // 异步rpc回调，需要提交到用户线程才能执行。
        // 这里批量提交的影响较小，因此选择批量提交
        ConcurrentUtils.tryCommit(session.localEventLoop(), () -> {
            for (RpcPromiseInfo rpcPromiseInfo : rpcPromiseInfoMap.values()) {
                ConcurrentUtils.safeExecute((Runnable) () -> rpcPromiseInfo.rpcCallback.onComplete(RpcResponse.SESSION_CLOSED));
            }
        });
    }
}
