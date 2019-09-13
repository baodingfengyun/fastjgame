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

package com.wjybxx.fastjgame.net.remote;

import com.wjybxx.fastjgame.concurrent.ListenableFuture;
import com.wjybxx.fastjgame.manager.NetManagerWrapper;
import com.wjybxx.fastjgame.manager.SocketSessionManager;
import com.wjybxx.fastjgame.misc.HostAndPort;
import com.wjybxx.fastjgame.net.NetContext;
import com.wjybxx.fastjgame.net.RoleType;
import com.wjybxx.fastjgame.net.SessionSenderMode;
import com.wjybxx.fastjgame.utils.EventLoopUtils;

import javax.annotation.Nonnull;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 连接的发起方建立的会话信息
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/4/27 10:00
 * github - https://github.com/hl845740757
 */
public class SocketC2SSession extends AbstractSocketSession {

    /**
     * 服务器地址
     */
    private final HostAndPort remoteAddress;
    /**
     * 会话状态：session连接成功以后，用户才能获取到session，因此这里初始化为true没有影响。
     */
    private final AtomicBoolean stateHolder = new AtomicBoolean(true);

    public SocketC2SSession(NetContext netContext, NetManagerWrapper netManagerWrapper,
                            long serverGuid, RoleType serverType, HostAndPort remoteAddress,
                            SessionSenderMode sessionSenderMode) {
        super(netContext, serverGuid, serverType, netManagerWrapper, sessionSenderMode);
        this.remoteAddress = remoteAddress;
    }

    @Nonnull
    @Override
    protected SocketSessionManager getSessionManager() {
        return managerWrapper.getSocketC2SSessionManager();
    }

    public HostAndPort remoteAddress() {
        return remoteAddress;
    }

    @Override
    public boolean isActive() {
        return stateHolder.get();
    }

    /**
     * 标记为已关闭
     */
    public void setClosed() {
        stateHolder.set(false);
    }

    @Override
    public ListenableFuture<?> close() {
        // 为什么不对它做优化了？ 因为他本身调用的频率就很低，平白无故的增加复杂度不值得。
        // 设置状态
        setClosed();
        // 可能是自己关闭，因此可能是当前线程，重复调用也必须发送NetEventLoop，否则可能看似关闭，实则还未关闭
        return EventLoopUtils.submitOrRun(netEventLoop(), () -> {
            managerWrapper.getSocketC2SSessionManager().removeSession(localGuid(), remoteGuid(), "close method");
        });
    }

    @Override
    public String toString() {
        return "SocketC2SSession{" +
                "localGuid=" + localGuid() +
                ", localRole=" + localRole() +
                ", remoteGuid=" + remoteGuid +
                ", remoteRole=" + remoteRole +
                ", remoteAddress=" + remoteAddress +
                ", active=" + stateHolder.get() +
                '}';
    }
}
