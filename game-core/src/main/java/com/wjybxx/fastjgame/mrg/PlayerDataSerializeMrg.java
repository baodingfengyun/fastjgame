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
import com.wjybxx.fastjgame.misc.PlatformType;
import com.wjybxx.fastjgame.protobuffer.p_common;
import com.wjybxx.fastjgame.scene.gameobjectdata.PlayerData;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * 玩家数据序列化控制器。
 * center与scene之间传递玩家数据是通过序列化的方式发送的。
 * 如果采用redis，也避免
 *
 * 这里的代码最好是想办法自动生成;
 * @author wjybxx
 * @version 1.0
 * date - 2019/6/23 12:18
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public class PlayerDataSerializeMrg {

    @Inject
    public PlayerDataSerializeMrg() {

    }

    // TODO 代码尽量多自动生成一些

    public PlayerData deserialize(p_common.p_player_data data) {
        PlayerData playerData = new PlayerData(data.getPlayerGuid());
        playerData.setPlatformType(PlatformType.forNumber(data.getPlatformNumber()));
        playerData.setLogicServerId(data.getLogicServerId());

        return playerData;
    }

    public p_common.p_player_data serialize(PlayerData playerData){
        p_common.p_player_data.Builder builder = p_common.p_player_data.newBuilder();
        builder.setPlayerGuid(playerData.getGuid());
        builder.setPlatformNumber(playerData.getPlatformType().getNumber());
        builder.setLogicServerId(playerData.getLogicServerId());

        return builder.build();
    }

}
