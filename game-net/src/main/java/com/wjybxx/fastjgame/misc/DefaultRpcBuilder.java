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

import com.wjybxx.fastjgame.net.common.RpcCall;
import com.wjybxx.fastjgame.net.common.RpcCallback;
import com.wjybxx.fastjgame.net.session.Session;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

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

    /**
     * 远程方法信息
     */
    private RpcCall<V> call;
    /**
     * 添加的回调
     */
    private RpcCallback<V> callback = null;

    /**
     * @param call 一般来讲，是用于转发的RpcCall
     */
    public DefaultRpcBuilder(RpcCall<V> call) {
        this.call = call;
    }

    /**
     * 该方法是生成的代码调用的。
     */
    public DefaultRpcBuilder(int methodKey, List<Object> methodParams, int lazyIndexes, int preIndexes) {
        this.call = new RpcCall<>(methodKey, methodParams, lazyIndexes, preIndexes);
    }

    @Override
    public final RpcBuilder<V> onSuccess(@Nonnull SucceededRpcCallback<V> callback) {
        addCallback(callback);
        return this;
    }

    @Override
    public final RpcBuilder<V> onFailure(@Nonnull FailedRpcCallback<V> callback) {
        addCallback(callback);
        return this;
    }

    @Override
    public final RpcBuilder<V> onComplete(@Nonnull RpcCallback<V> callback) {
        addCallback(callback);
        return this;
    }

    private void addCallback(final RpcCallback<V> newCallback) {
        // 多数情况下我们都只有一个回调
        if (callback == null) {
            callback = newCallback;
            return;
        }
        // 添加超过两次
        if (callback instanceof CompositeRpcCallback) {
            ((CompositeRpcCallback<V>) this.callback).addChild(newCallback);
        } else {
            // 添加的第二个回调
            callback = new CompositeRpcCallback<>(callback, newCallback);
        }
    }

    @Override
    public void send(@Nonnull Session session) throws IllegalStateException {
        Objects.requireNonNull(session, "session");
        session.send(call);
    }

    @Override
    public void broadcast(@Nonnull Iterable<Session> sessionGroup) throws IllegalStateException {
        Objects.requireNonNull(sessionGroup, "sessionGroup");
        for (Session session : sessionGroup) {
            Objects.requireNonNull(session, "session").send(call);
        }
    }

    @Override
    public final void call(@Nonnull Session session) {
        Objects.requireNonNull(session, "session");
        if (callback == null) {
            // 没有设置回调，使用通知代替rpc调用，对方不会返回结果
            session.send(call);
        } else {
            try {
                session.call(call, callback);
            } finally {
                // 一旦调用call，清除回调
                callback = null;
            }
        }
    }

    @Override
    public V syncCall(@Nonnull Session session) throws ExecutionException {
        Objects.requireNonNull(session, "session");
        return session.syncCall(call);
    }

    @Override
    public RpcCall<V> getCall() {
        return call;
    }

    @Override
    public RpcBuilder<V> router(RpcRouter<V> router) {
        this.call = router.route(call).getCall();
        return this;
    }
}
