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

package com.wjybxx.fastjgame.mrg;

import com.google.inject.Inject;
import com.google.protobuf.Message;
import com.google.protobuf.Message.Builder;
import com.google.protobuf.MessageLite;
import com.wjybxx.fastjgame.misc.SceneInCenterInfo;
import com.wjybxx.fastjgame.core.SceneWorldType;
import com.wjybxx.fastjgame.misc.WarzoneInCenterInfo;
import com.wjybxx.fastjgame.net.RpcResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.Optional;

/**
 * 中心服发送消息的辅助类，提供简单易使用的发送消息接口
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/6/22 23:29
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public class CenterSendMrg {

    private static final Logger logger = LoggerFactory.getLogger(CenterSendMrg.class);

    private final SceneInCenterInfoMrg sceneInCenterInfoMrg;
    private final WarzoneInCenterInfoMrg warzoneInCenterInfoMrg;

    @Inject
    public CenterSendMrg(SceneInCenterInfoMrg sceneInCenterInfoMrg, WarzoneInCenterInfoMrg warzoneInCenterInfoMrg) {
        this.sceneInCenterInfoMrg = sceneInCenterInfoMrg;
        this.warzoneInCenterInfoMrg = warzoneInCenterInfoMrg;
    }

    /**
     * 发送异步消息到指定guid的场景进程;
     *
     * @param worldGuid 场景worldGuid
     * @param msg       发送的消息
     */
    public void sendToScene(long worldGuid, @Nonnull Message msg) {
        SceneInCenterInfo sceneInfo = sceneInCenterInfoMrg.getSceneInfo(worldGuid);
        if (null != sceneInfo && sceneInfo.getSession() != null) {
            sceneInfo.getSession().send(msg);
        } else {
            logger.info("scene process {} is disconnect already. just discard msg.", worldGuid);
        }
    }

    public void sendToScene(long worldGuid, @Nonnull Builder builder) {
        sendToScene(worldGuid, builder.build());
    }

    /**
     * 发送异步消息到指定场景进程频道
     *
     * @param channelId 场景对应的频道id
     * @param msg       发送的消息
     */
    public void sendToScene(int channelId, @Nonnull Message msg) {
        SceneInCenterInfo sceneInfo = sceneInCenterInfoMrg.getSceneInfo(channelId);
        if (null != sceneInfo && sceneInfo.getSession() != null) {
            sceneInfo.getSession().send(msg);
        } else {
            logger.info("channel {} is disconnect already.", channelId);
        }
    }

    public void sendToScene(int channelId, @Nonnull Builder builder) {
        sendToScene(channelId, builder.build());
    }

    /**
     * 广播所有scene
     *
     * @param msg 广播消息
     */
    public void broadcastScene(Message msg) {
        for (SceneInCenterInfo sceneInCenterInfo : sceneInCenterInfoMrg.getAllSceneInfo()) {
            if (null == sceneInCenterInfo.getSession()) {
                continue;
            }
            sceneInCenterInfo.getSession().send(msg);
        }
    }

    /**
     * 广播所有本服场景
     *
     * @param msg 广播消息
     */
    public void broadcastSingleScene(Message msg) {
        for (SceneInCenterInfo sceneInCenterInfo : sceneInCenterInfoMrg.getAllSceneInfo()) {
            if (null == sceneInCenterInfo.getSession()) {
                continue;
            }
            if (sceneInCenterInfo.getWorldType() == SceneWorldType.SINGLE) {
                sceneInCenterInfo.getSession().send(msg);
            }
        }
    }

    /**
     * 广播所有跨服场景
     *
     * @param msg 广播消息
     */
    public void broadcastCrossScene(Message msg) {
        for (SceneInCenterInfo sceneInCenterInfo : sceneInCenterInfoMrg.getAllSceneInfo()) {
            if (null == sceneInCenterInfo.getSession()) {
                continue;
            }
            if (sceneInCenterInfo.getWorldType() == SceneWorldType.CROSS) {
                sceneInCenterInfo.getSession().send(msg);
            }
        }
    }

    /**
     * 同步rpc scene
     *
     * @param <T>       帮助外部强转
     * @param worldGuid 场景worldGuid
     * @param msg       发送的消息(请求)
     * @return rpc调用结果 如果{@link Optional#isPresent()} 为true，表示调用成功，否则表示失败
     */
    public <T extends MessageLite> RpcResponse syncRpcScene(long worldGuid, @Nonnull Message msg) {
        SceneInCenterInfo sceneInfo = sceneInCenterInfoMrg.getSceneInfo(worldGuid);
        if (null != sceneInfo && sceneInfo.getSession() != null) {
            return sceneInfo.getSession().syncRpcUninterruptibly(msg);
        } else {
            logger.info("scene process {} is disconnect already.", worldGuid);
            return RpcResponse.SESSION_CLOSED;
        }
    }

    /**
     * 同步rpc scene
     *
     * @param <T>       帮助外部强转
     * @param channelId 场景频道
     * @param msg       发送的消息(请求)
     * @return rpc调用结果 如果{@link Optional#isPresent()} 为true，表示调用成功，否则表示失败
     */
    public <T extends MessageLite> RpcResponse syncRpcScene(int channelId, @Nonnull Message msg) {
        SceneInCenterInfo sceneInfo = sceneInCenterInfoMrg.getSceneInfo(channelId);
        if (null != sceneInfo && sceneInfo.getSession() != null) {
            return sceneInfo.getSession().syncRpcUninterruptibly(msg);
        } else {
            logger.info("channel {} is disconnect already.", channelId);
            return RpcResponse.SESSION_CLOSED;
        }
    }

    /**
     * 发送异步消息到warzone(战区)
     *
     * @param msg 发送的消息
     */
    public void sendToWarzone(@Nonnull Message msg) {
        WarzoneInCenterInfo warzoneInCenterInfo = warzoneInCenterInfoMrg.getWarzoneInCenterInfo();
        if (null != warzoneInCenterInfo && warzoneInCenterInfo.getSession() != null) {
            warzoneInCenterInfo.getSession().send(msg);
        } else {
            logger.info("warzone is disconnect already.");
        }
    }

    public void sendToWarzone(@Nonnull Builder builder) {
        sendToWarzone(builder.build());
    }

}
