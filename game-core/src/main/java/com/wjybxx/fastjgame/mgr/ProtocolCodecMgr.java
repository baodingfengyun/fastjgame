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
import com.wjybxx.fastjgame.annotation.EventLoopSingleton;
import com.wjybxx.fastjgame.misc.MessageHashMappingStrategy;
import com.wjybxx.fastjgame.misc.MessageMapper;
import com.wjybxx.fastjgame.misc.MessageMappingStrategy;
import com.wjybxx.fastjgame.misc.ReflectBasedProtocolCodec;
import com.wjybxx.fastjgame.net.common.ProtocolCodec;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.HashMap;
import java.util.Map;

/**
 * 协议编解码管理器。World级别单例，不同的world可能有不同的需求。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/4/27 22:01
 * github - https://github.com/hl845740757
 */
@EventLoopSingleton
@NotThreadSafe
public final class ProtocolCodecMgr {

    // 进程内共用数据，不必每个实例一份儿
    private static final MessageMapper INNER_MESSAGE_MAPPER = MessageMapper.newInstance(new MessageHashMappingStrategy());
    private static final ProtocolCodec INNER_PROTOCOL_CODEC = ReflectBasedProtocolCodec.newInstance(INNER_MESSAGE_MAPPER);

    private final Map<String, ProtocolCodec> codecMapper = new HashMap<>();

    @Inject
    public ProtocolCodecMgr() {

    }

    /**
     * 获取服务器之间协议编解码器
     *
     * @return protocolCodec
     */
    public ProtocolCodec getInnerProtocolCodec() {
        return INNER_PROTOCOL_CODEC;
    }

    /**
     * 通过mappingStrategy和serializer注册codec
     *
     * @param name            codec的名字
     * @param mappingStrategy 该端口上使用到的消息
     * @throws Exception mapping error , or init exception
     */
    public void registerProtocolCodec(String name, MessageMappingStrategy mappingStrategy) throws Exception {
        registerProtocolCodec(name, ReflectBasedProtocolCodec.newInstance(MessageMapper.newInstance(mappingStrategy)));
    }

    /**
     * 注册protocolCodec
     *
     * @param name          codec的名字
     * @param protocolCodec codec辅助类
     */
    public void registerProtocolCodec(@Nonnull String name, @Nonnull ProtocolCodec protocolCodec) {
        if (codecMapper.containsKey(name)) {
            throw new IllegalArgumentException("duplicate protocolCodec name " + name);
        }
        codecMapper.put(name, protocolCodec);
    }

    /**
     * 获取protocolCodec
     *
     * @param name codec的名字
     * @return ProtocolCodec
     */
    public ProtocolCodec getProtocolCodec(@Nonnull String name) {
        ProtocolCodec protocolCodec = codecMapper.get(name);
        if (null == protocolCodec) {
            throw new IllegalArgumentException("protocolCodec " + name + " is not registered.");
        }
        return protocolCodec;
    }

}
