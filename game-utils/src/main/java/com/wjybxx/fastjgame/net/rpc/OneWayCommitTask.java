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
package com.wjybxx.fastjgame.net.rpc;

import com.wjybxx.fastjgame.net.session.Session;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.annotation.Nonnull;

/**
 * 单向消息提交任务
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/8
 * github - https://github.com/hl845740757
 */
public class OneWayCommitTask implements RpcProcessContext, CommitTask {

    /**
     * session - 包含协议分发器
     */
    private final Session session;
    /**
     * 单向消息的内容
     */
    private final Object message;

    public OneWayCommitTask(Session session, Object message) {
        this.session = session;
        this.message = message;
    }

    @Override
    public void run() {
        try {
            // 忽略结果，也避免创建不必要的promise
            session.config().dispatcher().post(this, (RpcMethodSpec) message);
        } catch (Throwable e) {
            // 直接抛出，交给执行者处理
            ExceptionUtils.rethrow(e);
        }
    }

    @Nonnull
    @Override
    public Session session() {
        return session;
    }

    @Override
    public boolean isRpc() {
        return false;
    }

    @Override
    public long requestGuid() {
        return 0;
    }

    @Override
    public boolean isSyncRpc() {
        return false;
    }
}
