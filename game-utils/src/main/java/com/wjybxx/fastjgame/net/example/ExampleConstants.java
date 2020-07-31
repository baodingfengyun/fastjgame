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
import com.wjybxx.fastjgame.net.serialization.HashTypeMappingStrategy;
import com.wjybxx.fastjgame.net.serialization.JsonSerializer;
import com.wjybxx.fastjgame.net.type.DefaultTypeModelMapper;
import com.wjybxx.fastjgame.net.type.TypeMappingStrategy;
import com.wjybxx.fastjgame.net.type.TypeModelMapper;

import java.util.stream.Collectors;

/**
 * 测试用例的常量
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/6
 * github - https://github.com/hl845740757
 */
public final class ExampleConstants {

    public static TypeMappingStrategy typeMappingStrategy = new HashTypeMappingStrategy();

    public static TypeModelMapper typeModelMapper = DefaultTypeModelMapper.newInstance(
            CodecScanner.getAllCustomCodecClass().stream()
                    .map(typeMappingStrategy::mapping)
                    .collect(Collectors.toList())
    );

    /**
     * 测试用例使用的codec
     */
    public static final JsonSerializer JSON_SERIALIZER = JsonSerializer.newInstance(typeModelMapper);
    public static final BinarySerializer BINARY_SERIALIZER = BinarySerializer.newInstance(typeModelMapper);

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
