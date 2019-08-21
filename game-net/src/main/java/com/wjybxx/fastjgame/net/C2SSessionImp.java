/*
 * Copyright 2019 wjybxx
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wjybxx.fastjgame.net;

import com.wjybxx.fastjgame.concurrent.ListenableFuture;
import com.wjybxx.fastjgame.manager.NetConfigManager;
import com.wjybxx.fastjgame.manager.NetManagerWrapper;
import com.wjybxx.fastjgame.manager.SessionManager;
import com.wjybxx.fastjgame.misc.HostAndPort;
import com.wjybxx.fastjgame.utils.EventLoopUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 客户端到服务器的会话信息
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/4/27 10:00
 * github - https://github.com/hl845740757
 */
public class C2SSessionImp extends AbstractSession implements C2SSession {

    private static final Logger logger = LoggerFactory.getLogger(C2SSessionImp.class);

    /** 未激活状态 */
    private static final int ST_INACTIVE = 0;
    /** 激活状态 */
    private static final int ST_ACTIVE = 1;
    /** 已关闭 */
    private static final int ST_CLOSED = 2;

    private final NetContext netContext;
    private final NetManagerWrapper netManagerWrapper;

    /**
     * 服务器唯一标识(会话id)
     */
    private final long serverGuid;
    /**
     * 服务器类型
     */
    private final RoleType serverType;
    /**
     * 服务器地址
     */
    private final HostAndPort remoteAddress;
    /**
     * 会话是否已激活，客户端会话是预先创建的，因此刚创建的时候是未激活的，当且成功建立连接时，才会激活。
     */
    private final AtomicInteger stateHolder = new AtomicInteger(ST_INACTIVE);

    public C2SSessionImp(NetContext netContext, NetManagerWrapper netManagerWrapper,
                         long serverGuid, RoleType serverType, HostAndPort remoteAddress) {
        this.netContext = netContext;
        this.netManagerWrapper = netManagerWrapper;
        this.serverGuid = serverGuid;
        this.serverType = serverType;
        this.remoteAddress = remoteAddress;
    }

    @Override
    public NetContext netContext() {
        return netContext;
    }

    @Override
    protected NetConfigManager getNetConfigManager() {
        return netManagerWrapper.getNetConfigManager();
    }

    @Override
    protected SessionManager getSessionManager() {
        return netManagerWrapper.getC2SSessionManager();
    }

    @Override
    public long remoteGuid() {
        return serverGuid;
    }

    @Override
    public RoleType remoteRole() {
        return serverType;
    }

    @Override
    public HostAndPort remoteAddress() {
        return remoteAddress;
    }

    /** 标记为已关闭 */
    public void setClosed() {
        assert netContext.netEventLoop().inEventLoop();
        stateHolder.set(ST_CLOSED);
    }

    /**
     * 尝试激活会话，由于可能与{@link #close()}没有竞争。因为用户在激活之前其它线程是无法获取到该Session对象的。
     * @return 如果成功设置为激活状态则返回true(激活方法只应该调用一次)
     */
    public boolean tryActive() {
        assert netContext.netEventLoop().inEventLoop();
        return stateHolder.compareAndSet(ST_INACTIVE, ST_ACTIVE);
    }

    @Override
    public String toString() {
        return "C2SSessionImp{" +
                "localGuid=" + localGuid() +
                ", localRole=" + localRole() +
                ", serverGuid=" + serverGuid +
                ", serverType=" + serverType +
                ", remoteAddress=" + remoteAddress +
                ", active=" + stateHolder.get() +
                '}';
    }

    @Override
    public boolean isActive() {
        return stateHolder.get() == ST_ACTIVE;
    }

    @Override
    public ListenableFuture<?> close() {
        // 为什么不对它做优化了？ 因为他本身调用的频率就很低，平白无故的增加复杂度不值得。
        // 设置状态
        setClosed();
        // 可能是自己关闭，因此可能是当前线程，重复调用也必须发送NetEventLoop，否则可能看似关闭，实则还未关闭
        return EventLoopUtils.submitOrRun(netContext.netEventLoop(), () -> {
            netManagerWrapper.getC2SSessionManager().removeSession(localGuid(), remoteGuid(), "close method");
        });
    }
}
