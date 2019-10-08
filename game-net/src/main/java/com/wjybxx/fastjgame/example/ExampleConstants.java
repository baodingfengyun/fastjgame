/*
 *    Copyright 2019 wjybxx
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.wjybxx.fastjgame.example;

import com.wjybxx.fastjgame.eventloop.NetEventLoopGroup;
import com.wjybxx.fastjgame.eventloop.NetEventLoopGroupBuilder;
import com.wjybxx.fastjgame.misc.JsonBasedProtocolCodec;
import com.wjybxx.fastjgame.misc.MessageMapper;
import com.wjybxx.fastjgame.misc.ReflectBasedProtocolCodec;

/**
 * 测试用例的常量
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/6
 * github - https://github.com/hl845740757
 */
public final class ExampleConstants {

    public static final long CLIENT_GUID = 10001;
    public static final long SERVER_GUID = 20001;
    /**
     * sessionId
     */
    public static final String SESSION_ID = "10001-20001";
    /**
     * 空的token
     */
    public static final byte[] EMPTY_TOKEN = new byte[0];

    /**
     * 测试用例使用的codec
     */
    public static final MessageMapper messageMapper = MessageMapper.newInstance(new ExampleHashMappingStrategy());
    public static final JsonBasedProtocolCodec jsonBasedCodec = new JsonBasedProtocolCodec(messageMapper);
    public static final ReflectBasedProtocolCodec reflectBasedCodec = ReflectBasedProtocolCodec.newInstance(messageMapper);

    public static final NetEventLoopGroup netEventLoop = new NetEventLoopGroupBuilder().build();
    /**
     * tcp端口
     */
    public static final int tcpPort = 23333;
    /**
     * http端口
     */
    public static final int httpPort = 54321;

    private ExampleConstants() {

    }
}
