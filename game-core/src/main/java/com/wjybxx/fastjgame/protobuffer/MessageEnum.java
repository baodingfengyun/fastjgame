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

package com.wjybxx.fastjgame.protobuffer;

/**
 * 消息枚举，自动生成的文件。
 * 工具来自于项目: - https://github.com/hl845740757/netty-any-protobuf
 *
 * @author wjybxx
 * @version 1.0
 * github - https://github.com/hl845740757
 */
public enum MessageEnum {

    P_CENTER_SINGLE_SCENE_HELLO(1309476611,"com.wjybxx.fastjgame.protobuffer","p_center_scene","p_center_single_scene_hello"),
    P_CENTER_SINGLE_SCENE_HELLO_RESULT(49177817,"com.wjybxx.fastjgame.protobuffer","p_center_scene","p_center_single_scene_hello_result"),
    P_CENTER_CROSS_SCENE_HELLO(431992165,"com.wjybxx.fastjgame.protobuffer","p_center_scene","p_center_cross_scene_hello"),
    P_CENTER_CROSS_SCENE_HELLO_RESULT(-767022025,"com.wjybxx.fastjgame.protobuffer","p_center_scene","p_center_cross_scene_hello_result"),
    P_CENTER_WARZONE_HELLO(195173740,"com.wjybxx.fastjgame.protobuffer","p_center_warzone","p_center_warzone_hello"),
    P_CENTER_WARZONE_HELLO_RESULT(-1192233392,"com.wjybxx.fastjgame.protobuffer","p_center_warzone","p_center_warzone_hello_result"),
    P_PLAYER_DATA(1433200889,"com.wjybxx.fastjgame.protobuffer","p_common","p_player_data"),
    P_SCENE_PLAYER_DATA(-82942714,"com.wjybxx.fastjgame.protobuffer","p_scene_player","p_scene_player_data"),
    P_SCENE_NPC_DATA(-1692854198,"com.wjybxx.fastjgame.protobuffer","p_scene_player","p_scene_npc_data"),
    P_SCENE_PET_DATA(1106856140,"com.wjybxx.fastjgame.protobuffer","p_scene_player","p_scene_pet_data"),
    P_NOTIFY_PLAYER_OTHERS_IN(-2094253302,"com.wjybxx.fastjgame.protobuffer","p_scene_player","p_notify_player_others_in"),
    P_NOTIFY_PLAYER_OTHERS_OUT(-497336823,"com.wjybxx.fastjgame.protobuffer","p_scene_player","p_notify_player_others_out"),
    P_CENTER_COMMAND_SINGLE_SCENE_START(22887687,"com.wjybxx.fastjgame.protobuffer","p_sync_center_scene","p_center_command_single_scene_start"),
    P_CENTER_COMMAND_SINGLE_SCENE_START_RESULT(345645653,"com.wjybxx.fastjgame.protobuffer","p_sync_center_scene","p_center_command_single_scene_start_result"),
    P_CENTER_COMMAND_SINGLE_SCENE_ACTIVE_REGIONS(938600641,"com.wjybxx.fastjgame.protobuffer","p_sync_center_scene","p_center_command_single_scene_active_regions"),
    P_CENTER_COMMAND_SINGLE_SCENE_ACTIVE_REGIONS_RESULT(-233197221,"com.wjybxx.fastjgame.protobuffer","p_sync_center_scene","p_center_command_single_scene_active_regions_result"),
    P_HELLOWORLD(1136092463,"com.wjybxx.fastjgame.protobuffer","p_test","p_helloworld"),
    P_SS(3431567,"com.wjybxx.fastjgame.protobuffer","p_test","p_ss"),
    ;
    /**
     * 消息id，必须唯一
     */
    private final int messageId;
    /**
     * java包名
     */
    private final String javaPackageName;

    /**
     * java外部类名字
     */
    private final String javaOuterClassName;

    /**
     * 消息名(类简单名)
     * {@link Class#getSimpleName()}
     */
    private final String messageName;

    MessageEnum(int messageId, String javaPackageName, String javaOuterClassName, String messageName) {
        this.messageId = messageId;
        this.javaPackageName = javaPackageName;
        this.javaOuterClassName = javaOuterClassName;
        this.messageName = messageName;
    }

    public int getMessageId() {
        return messageId;
    }

    public String getJavaPackageName() {
        return javaPackageName;
    }

    public String getJavaOuterClassName() {
        return javaOuterClassName;
    }

    public String getMessageName() {
        return messageName;
    }

    @Override
    public String toString() {
        return "MessageEnum{" +
                "messageId=" + messageId +
                ", javaPackageName='" + javaPackageName + '\'' +
                ", javaOuterClassName='" + javaOuterClassName + '\'' +
                ", messageName='" + messageName + '\'' +
                '}';
    }
}
