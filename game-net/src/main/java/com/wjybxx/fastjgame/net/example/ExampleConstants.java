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
package com.wjybxx.fastjgame.net.example;

import com.wjybxx.fastjgame.net.binary.BinarySerializer;
import com.wjybxx.fastjgame.net.binary.CodecScanner;
import com.wjybxx.fastjgame.net.eventloop.NetEventLoopGroup;
import com.wjybxx.fastjgame.net.eventloop.NetEventLoopGroupBuilder;
import com.wjybxx.fastjgame.net.serialization.HashMessageMappingStrategy;
import com.wjybxx.fastjgame.net.serialization.JsonSerializer;

/**
 * 测试用例的常量
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/6
 * github - https://github.com/hl845740757
 */
public final class ExampleConstants {

    /**
     * 测试用例使用的codec
     */
    public static final JsonSerializer JSON_SERIALIZER = JsonSerializer.newInstance(CodecScanner.getAllCustomCodecClass(), new HashMessageMappingStrategy());
    public static final BinarySerializer BINARY_SERIALIZER = BinarySerializer.newInstance(new HashMessageMappingStrategy());

    public static final NetEventLoopGroup netEventLoop = new NetEventLoopGroupBuilder()
            .setWorkerGroupThreadNum(2)
            .build();
    /**
     * tcp端口
     */
    public static final int tcpPort = 23333;

    private ExampleConstants() {

    }
}
