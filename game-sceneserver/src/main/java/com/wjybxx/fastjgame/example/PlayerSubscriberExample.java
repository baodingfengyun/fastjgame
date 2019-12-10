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

package com.wjybxx.fastjgame.example;

import com.google.protobuf.Message;
import com.wjybxx.fastjgame.annotation.PlayerEventSubscribe;
import com.wjybxx.fastjgame.gameobject.Player;
import com.wjybxx.fastjgame.misc.DefaultPlayerEventDispatcher;

import static com.wjybxx.fastjgame.protobuffer.p_common.p_player_data;
import static com.wjybxx.fastjgame.protobuffer.p_scene_player.p_scene_npc_data;
import static com.wjybxx.fastjgame.protobuffer.p_scene_player.p_scene_pet_data;

/**
 * 玩家事件(消息)示例
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/25
 * github - https://github.com/hl845740757
 */
public class PlayerSubscriberExample {

    public static void main(String[] args) {
        final DefaultPlayerEventDispatcher dispatcher = new DefaultPlayerEventDispatcher();
        PlayerSubscriberExamplePlayerEventRegister.register(dispatcher,
                new PlayerSubscriberExample());

        Player player = new Player(null, null, null, null);
        dispatcher.post(player, p_player_data
                .newBuilder()
                .setPlayerGuid(10000).build());

        dispatcher.post(player, p_scene_npc_data
                .newBuilder()
                .setNpcId(1)
                .setNpcGuid(10000).build());

        dispatcher.post(player, p_scene_pet_data
                .newBuilder()
                .setPetId(1)
                .setPetGuid(10000).build());
    }

    @PlayerEventSubscribe
    public void onMessage(Player player, p_player_data data) {
        System.out.println("onEvent\n" + data.toString());
    }

    @PlayerEventSubscribe(onlySubEvents = true, subEvents = {
            p_scene_npc_data.class,
            p_scene_pet_data.class
    })
    public void onNpcOrPetMessage(Player player, Message data) {
        System.out.println("onEvent\n" + data.toString());
    }


}
