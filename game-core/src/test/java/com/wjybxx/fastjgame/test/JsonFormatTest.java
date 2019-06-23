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

package com.wjybxx.fastjgame.test;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.wjybxx.fastjgame.protobuffer.p_center_scene;
import com.wjybxx.fastjgame.protobuffer.p_center_scene.p_center_cross_scene_hello;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2019/6/22 11:06
 * github - https://github.com/hl845740757
 */
public class JsonFormatTest {

    public static void main(String[] args) throws InvalidProtocolBufferException {
        p_center_cross_scene_hello.Builder builder = p_center_cross_scene_hello.newBuilder();
        builder.setPlatformNumber(1);
        builder.setServerId(1);
        p_center_cross_scene_hello msg = builder.build();

        // omittingInsignificantWhitespace() 不打印换行符
        String json = JsonFormat.printer().print(msg);
        System.out.println(json);

        p_center_cross_scene_hello.Builder jsonBuilder = p_center_cross_scene_hello.newBuilder();
        JsonFormat.parser().merge(json, jsonBuilder);
        System.out.println(jsonBuilder);
    }
}
