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
import com.wjybxx.fastjgame.core.SceneInCenterInfo;
import com.wjybxx.fastjgame.core.SceneProcessType;
import com.wjybxx.fastjgame.core.WarzoneInCenterInfo;
import com.wjybxx.fastjgame.mrg.async.C2SSessionMrg;
import com.wjybxx.fastjgame.mrg.sync.SyncC2SSessionMrg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.Optional;

/**
 * 中心服发送消息的辅助类，提供简单易使用的发送消息接口
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

    /**
     * center主动连接scene和warzone，
     * 因此scene和warzone是服务器端，center是会话的客户端。
     */
    private final C2SSessionMrg c2SSessionMrg;
    /**
     * center主动连接scene，因此scene是服务端，center是客户端。
     */
    private final SyncC2SSessionMrg syncC2SSessionMrg;

    @Inject
    public CenterSendMrg(SceneInCenterInfoMrg sceneInCenterInfoMrg, WarzoneInCenterInfoMrg warzoneInCenterInfoMrg, C2SSessionMrg c2SSessionMrg, SyncC2SSessionMrg syncC2SSessionMrg) {
        this.sceneInCenterInfoMrg = sceneInCenterInfoMrg;
        this.warzoneInCenterInfoMrg = warzoneInCenterInfoMrg;
        this.c2SSessionMrg = c2SSessionMrg;
        this.syncC2SSessionMrg = syncC2SSessionMrg;
    }

    /**
     * 发送异步消息到指定guid的场景进程;
     * @param processGuid 场景进程guid
     * @param msg 发送的消息
     */
    public void sendToScene(long processGuid, @Nonnull Message msg){
        SceneInCenterInfo sceneInfo = sceneInCenterInfoMrg.getSceneInfo(processGuid);
        if (null != sceneInfo){
            c2SSessionMrg.send(processGuid, msg);
        } else {
            logger.info("scene process {} is disconnect already. just discard msg.", processGuid);
        }
    }

    public void sendToScene(long processGuid, @Nonnull Builder builder){
        sendToScene(processGuid, builder.build());
    }

    /**
     * 发送异步消息到指定场景进程频道
     * @param channelId 场景对应的频道id
     * @param msg 发送的消息
     */
    public void sendToScene(int channelId, @Nonnull Message msg){
        SceneInCenterInfo sceneInfo = sceneInCenterInfoMrg.getSceneInfo(channelId);
        if (null != sceneInfo){
            c2SSessionMrg.send(sceneInfo.getSceneProcessGuid(), msg);
        } else {
            logger.info("channel {} is disconnect already.", channelId);
        }
    }

    public void sendToScene(int channelId, @Nonnull Builder builder){
        sendToScene(channelId, builder.build());
    }

    /**
     * 广播所有scene
     * @param msg 广播消息
     */
    public void broadcastScene(Message msg){
        for (SceneInCenterInfo sceneInCenterInfo:sceneInCenterInfoMrg.getAllSceneInfo()){
            c2SSessionMrg.send(sceneInCenterInfo.getSceneProcessGuid(), msg);
        }
    }

    /**
     * 广播所有本服场景
     * @param msg 广播消息
     */
    public void broadcastSingleScene(Message msg){
        for (SceneInCenterInfo sceneInCenterInfo:sceneInCenterInfoMrg.getAllSceneInfo()){
            if (sceneInCenterInfo.getProcessType() == SceneProcessType.SINGLE){
                c2SSessionMrg.send(sceneInCenterInfo.getSceneProcessGuid(), msg);
            }
        }
    }

    /**
     * 广播所有跨服场景
     * @param msg 广播消息
     */
    public void broadcastCrossScene(Message msg){
        for (SceneInCenterInfo sceneInCenterInfo:sceneInCenterInfoMrg.getAllSceneInfo()){
            if (sceneInCenterInfo.getProcessType() == SceneProcessType.CROSS){
                c2SSessionMrg.send(sceneInCenterInfo.getSceneProcessGuid(), msg);
            }
        }
    }

    /**
     * 同步rpc scene
     * @param processGuid 场景进程guid
     * @param msg 发送的消息(请求)
     * @param <T> 帮助外部强转
     * @return rpc调用结果 如果{@link Optional#isPresent()} 为true，表示调用成功，否则表示失败
     */
    public <T extends MessageLite> Optional<T> callScene(long processGuid, @Nonnull Message msg){
        SceneInCenterInfo sceneInfo = sceneInCenterInfoMrg.getSceneInfo(processGuid);
        if (null != sceneInfo){
            return syncC2SSessionMrg.call(processGuid, msg);
        } else {
            logger.info("scene process {} is disconnect already.", processGuid);
            return Optional.empty();
        }
    }

    /**
     * 同步rpc scene
     * @param channelId 场景频道
     * @param msg 发送的消息(请求)
     * @param <T> 帮助外部强转
     * @return rpc调用结果 如果{@link Optional#isPresent()} 为true，表示调用成功，否则表示失败
     */
    public <T extends MessageLite> Optional<T> callScene(int channelId, @Nonnull Message msg){
        SceneInCenterInfo sceneInfo = sceneInCenterInfoMrg.getSceneInfo(channelId);
        if (null != sceneInfo){
            return syncC2SSessionMrg.call(sceneInfo.getSceneProcessGuid(), msg);
        } else {
            logger.info("channel {} is disconnect already.", channelId);
            return Optional.empty();
        }
    }

    /**
     * 发送异步消息到warzone(战区)
     * @param msg 发送的消息
     */
    public void sendToWarzone(@Nonnull Message msg){
        WarzoneInCenterInfo warzoneInCenterInfo = warzoneInCenterInfoMrg.getWarzoneInCenterInfo();
        if (null != warzoneInCenterInfo){
            c2SSessionMrg.send(warzoneInCenterInfo.getWarzoneProcessGuid(), msg);
        } else {
            logger.info("warzone is disconnect already.");
        }
    }

    public void sendToWarzone(@Nonnull Builder builder){
        sendToWarzone(builder.build());
    }

}
