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
import com.wjybxx.fastjgame.annotation.WorldSingleton;
import com.wjybxx.fastjgame.misc.ProtoBufHashMappingStrategy;
import com.wjybxx.fastjgame.net.*;
import com.wjybxx.fastjgame.utils.GameUtils;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.HashMap;
import java.util.Map;

/**
 * CodecHelper管理器。World级别单例，不同的world可能有不同的需求。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/4/27 22:01
 * github - https://github.com/hl845740757
 */
@WorldSingleton
@NotThreadSafe
public final class CodecHelperMrg {

    // 进程内共用数据，不必每个实例一份儿
    private static final MessageMapper INNER_MESSAGE_MAPPER = MessageMapper.newInstance(new ProtoBufHashMappingStrategy());
    private static final MessageSerializer INNER_SERIALIZER = new ProtoBufMessageSerializer();
    private static final CodecHelper INNER_CODEC_HELPER = CodecHelper.newInstance(INNER_MESSAGE_MAPPER, INNER_SERIALIZER);

    private final Map<String, CodecHelper> codecMapper = new HashMap<>();

    @Inject
    public CodecHelperMrg() throws Exception {
        // 注册内部通信的CodecHelper
        registerCodecHelper(GameUtils.INNER_CODEC_NAME, INNER_CODEC_HELPER);
    }

    public CodecHelper getInnerCodecHolder() {
        return getCodecHelper(GameUtils.INNER_CODEC_NAME);
    }

    /**
     * 通过mappingStrategy和serializer注册codec
     * @param name codec的名字
     * @param mappingStrategy 消息映射策略
     * @param messageSerializer 消息序列化方式
     * @throws Exception mapping error , or init exception
     */
    public void registerCodecHelper(String name, MessageMappingStrategy mappingStrategy, MessageSerializer messageSerializer) throws Exception {
        registerCodecHelper(name, CodecHelper.newInstance(mappingStrategy, messageSerializer));
    }

    /**
     * 注册codecHelper
     * @param name codec的名字
     * @param codecHelper codec辅助类
     */
    public void registerCodecHelper(@Nonnull String name, @Nonnull CodecHelper codecHelper){
        if (codecMapper.containsKey(name)){
            throw new IllegalArgumentException("duplicate codecHelper name "+name);
        }
        codecMapper.put(name,codecHelper);
    }

    /**
     * 获取codecHelper
     * @param name codec的名字
     * @return CodecHelper
     */
    public CodecHelper getCodecHelper(@Nonnull String name){
        CodecHelper codecHelper = codecMapper.get(name);
        if (null == codecHelper){
            throw new IllegalArgumentException("codecHelper " + name + " is not registered.");
        }
        return codecHelper;
    }


}
