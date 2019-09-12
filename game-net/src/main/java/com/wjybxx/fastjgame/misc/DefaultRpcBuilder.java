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

package com.wjybxx.fastjgame.misc;

import com.wjybxx.fastjgame.net.RpcCallback;
import com.wjybxx.fastjgame.net.RpcResponse;
import com.wjybxx.fastjgame.net.Session;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.List;

/**
 * {@link RpcBuilder}的默认实现
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/23
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public class DefaultRpcBuilder<V> implements RpcBuilder<V> {

    private static final int SHARE_MODE_ANY = 0;
    private static final int SHARE_MODE_SEND = 1;
    private static final int SHARE_MODE_NONE = 2;

    /**
     * 远程方法信息
     */
    private final RpcCall call;
    /**
     * 添加的回调
     */
    private RpcCallback callback = null;
    /**
     * 共享模式
     */
    private int shareMode = SHARE_MODE_ANY;

    public DefaultRpcBuilder(int methodKey, List<Object> methodParams) {
        this.call = new RpcCall(methodKey, methodParams);
    }

    @Override
    public final RpcBuilder<V> ifSuccess(@Nonnull SucceedRpcCallback<V> callback) {
        addCallback(callback);
        return this;
    }

    @Override
    public final RpcBuilder<V> ifFailure(@Nonnull FailedRpcCallback callback) {
        addCallback(callback);
        return this;
    }

    @Override
    public final RpcBuilder<V> any(@Nonnull RpcCallback callback) {
        addCallback(callback);
        return this;
    }

    private void addCallback(final RpcCallback newCallback) {
        // 多数情况下我们都只有一个回调
        if (callback == null) {
            callback = newCallback;
            return;
        }
        // 添加超过两次
        if (callback instanceof CompositeRpcCallback) {
            ((CompositeRpcCallback) this.callback).any(newCallback);
        } else {
            // 添加的第二个回调
            callback = new CompositeRpcCallback<>(callback, newCallback);
        }
    }

    @Override
    public void send(@Nullable Session session) throws IllegalStateException {
        ensureSendAvailable();
        if (session != null) {
            session.send(call);
        }
        // else do nothing
    }

    @Override
    public void broadcast(@Nullable Iterable<Session> sessionIterable) throws IllegalStateException {
        ensureSendAvailable();
        if (sessionIterable == null) {
            return;
        }
        for (Session session : sessionIterable) {
            if (session != null) {
                session.send(call);
            }
        }
    }

    /**
     * 确保可发送send请求
     */
    private void ensureSendAvailable() {
        if (shareMode > SHARE_MODE_SEND) {
            throw new IllegalStateException("this builder does not support reuse!");
        }
        shareMode = SHARE_MODE_SEND;
    }

    /**
     * 确保可发送rpc请求
     */
    private void ensureRpcAvailable() {
        if (shareMode > SHARE_MODE_ANY) {
            throw new IllegalStateException("this builder does not support reuse!");
        }
        shareMode = SHARE_MODE_NONE;
    }

    @Override
    public final void call(@Nullable Session session) {
        ensureRpcAvailable();
        if (session == null) {
            if (callback != null) {
                // session不存在，安全的失败
                callback.onComplete(RpcResponse.SESSION_NULL);
            }
        } else {
            if (callback == null) {
                // 没有设置回调，使用通知代替rpc调用，对方不会返回结果
                session.send(call);
            } else {
                // 设置了回调，走rpc，对方一定会返回一个值
                session.call(call, callback);
            }
        }
    }

    @Nonnull
    @Override
    public final RpcResponse sync(@Nullable Session session) throws IllegalStateException {
        ensureRpcAvailable();
        final RpcResponse response;
        if (session == null) {
            // session不存在，安全的失败
            response = RpcResponse.SESSION_NULL;
        } else {
            response = session.sync(call);
        }
        return response;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public V syncCall(@Nullable Session session) {
        final RpcResponse rpcResponse = sync(session);
        if (rpcResponse.isSuccess()) {
            return (V) rpcResponse.getBody();
        } else {
            return null;
        }
    }

    // ---------------------------------------------------- 分割线 ----------------------------------------------

    @Override
    public void sendImmediately(@Nullable Session session) throws IllegalStateException {
        ensureSendAvailable();
        if (session != null) {
            session.sendImmediately(call);
        }
    }

    @Override
    public void broadcastImmediately(@Nullable Iterable<Session> sessionIterable) throws IllegalStateException {
        ensureSendAvailable();
        if (sessionIterable == null) {
            return;
        }
        for (Session session : sessionIterable) {
            if (session != null) {
                session.sendImmediately(call);
            }
        }
    }

    @Override
    public void callImmediately(@Nullable Session session) throws IllegalStateException {
        ensureRpcAvailable();
        if (session == null) {
            if (callback != null) {
                // session不存在，安全的失败
                callback.onComplete(RpcResponse.SESSION_NULL);
            }
        } else {
            if (callback == null) {
                // 没有设置回调，使用通知代替rpc调用，对方不会返回结果
                session.sendImmediately(call);
            } else {
                // 设置了回调，走rpc，对方一定会返回一个值
                session.callImmediately(call, callback);
            }
        }
    }

    @Nonnull
    @Override
    public RpcResponse syncImmediately(@Nullable Session session) throws IllegalStateException {
        ensureRpcAvailable();
        final RpcResponse response;
        if (session == null) {
            // session不存在，安全的失败
            response = RpcResponse.SESSION_NULL;
        } else {
            response = session.syncImmediately(call);
        }
        return response;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public V syncCallImmediately(@Nullable Session session) throws IllegalStateException {
        final RpcResponse rpcResponse = syncImmediately(session);
        if (rpcResponse.isSuccess()) {
            return (V) rpcResponse.getBody();
        } else {
            return null;
        }
    }
}
