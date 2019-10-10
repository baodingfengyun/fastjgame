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

package com.wjybxx.fastjgame.mgr;

import com.google.inject.Inject;
import com.google.protobuf.Message;
import com.wjybxx.fastjgame.misc.PlatformType;
import com.wjybxx.fastjgame.net.session.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 战区发送信息的辅助类
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/6/23 23:53
 * github - https://github.com/hl845740757
 */
public class WarzoneSendMgr {

    private static final Logger logger = LoggerFactory.getLogger(WarzoneSendMgr.class);

    private final CenterInWarzoneInfoMgr centerInWarzoneInfoMgr;

    @Inject
    public WarzoneSendMgr(CenterInWarzoneInfoMgr centerInWarzoneInfoMgr) {
        this.centerInWarzoneInfoMgr = centerInWarzoneInfoMgr;
    }

    /**
     * 发送消息到某个center服
     *
     * @param platformType center服所在的运营平台
     * @param serverId     服id
     */
    public void sendToCenter(PlatformType platformType, int serverId, Message msg) {
        Session session = centerInWarzoneInfoMgr.getCenterSession(platformType, serverId);
        if (null != session) {
            session.send(msg);
        } else {
            logger.info("try send msg to {}-{}, but already disconnect", platformType, serverId);
        }
    }

    public void sendToCenter(PlatformType platformType, int serverId, Message.Builder builder) {
        sendToCenter(platformType, serverId, builder.build());
    }

}