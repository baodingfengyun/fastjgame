/*
 *    Copyright 2019 wjybxx
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.wjybxx.fastjgame.misc;

import com.wjybxx.fastjgame.net.common.RpcCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 为多个RpcCallback提供一个单一的视图。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/19
 * github - https://github.com/hl845740757
 */
public final class CompositeRpcCallback<V> implements RpcCallback<V> {

    private static final Logger logger = LoggerFactory.getLogger(CompositeRpcCallback.class);

    private final List<RpcCallback<V>> children = new ArrayList<>(2);

    CompositeRpcCallback(RpcCallback<V> first, RpcCallback<V> second) {
        children.add(first);
        children.add(second);
    }

    @Override
    public void onComplete(V result, Throwable cause) {
        for (RpcCallback<V> rpcCallback : children) {
            try {
                rpcCallback.onComplete(result, cause);
            } catch (Throwable e) {
                logger.warn("Child onComplete caught exception!", e);
            }
        }
    }

    void addChild(RpcCallback<V> rpcCallback) {
        children.add(rpcCallback);
    }
}
