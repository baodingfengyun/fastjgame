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

package com.wjybxx.fastjgame.net.common;

import com.wjybxx.fastjgame.concurrent.async.AsyncMethodException;
import com.wjybxx.fastjgame.concurrent.async.AsyncMethodHandle;
import com.wjybxx.fastjgame.concurrent.async.AsyncMethodListenable;
import com.wjybxx.fastjgame.net.session.Session;

import java.util.Collection;

/**
 * rpc方法句柄
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/1/5 20:58
 * github - https://github.com/hl845740757
 */
public interface RpcMethodHandle<V> extends AsyncMethodHandle<Session, V> {

    @Override
    AsyncMethodListenable<V> call(Session typeObj);

    @Override
    V syncCall(Session session) throws AsyncMethodException;

    @Override
    RpcMethodHandle<V> invoke(Session session);

    @Override
    RpcMethodHandle<V> invoke(Collection<Session> sessionCollection);
}