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

import com.wjybxx.fastjgame.annotation.PlayerMessageSubscribe;
import com.wjybxx.fastjgame.gameobject.Player;
import com.wjybxx.fastjgame.misc.DefaultPlayerMessageDispatcher;
import com.wjybxx.fastjgame.protobuffer.p_common;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/25
 * github - https://github.com/hl845740757
 */
public class MessageSubscriberExample {

    public static void main(String[] args) {
        final DefaultPlayerMessageDispatcher dispatcher = new DefaultPlayerMessageDispatcher();
        MessageSubscriberExampleMsgFunRegister.register(dispatcher,
                new MessageSubscriberExample());

        Player player = new Player(null, null, null, null);
        final p_common.p_player_data.Builder builder = p_common.p_player_data.newBuilder();
        builder.setPlayerGuid(10000);
        dispatcher.post(player, builder.build());
    }

    @PlayerMessageSubscribe
    public void onMessage(Player player, p_common.p_player_data data) {
        System.out.println("onMessage " + data.toString());
    }
}
